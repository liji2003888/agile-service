import React, { Component } from 'react';
import { observer } from 'mobx-react';
import { Modal, Form, Input } from 'choerodon-ui';
import { stores } from '@choerodon/boot';
import { epicApi, issueApi, fieldApi } from '@/api';
import StoryMapStore from '../../../../../stores/project/StoryMap/StoryMapStore';

const { AppState } = stores;
const { Sidebar } = Modal;
const FormItem = Form.Item;
const { TextArea } = Input;


class CreateEpicModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
    };
  }

  componentDidUpdate(prevProps) {
    // eslint-disable-next-line react/destructuring-assignment
    if (this.props.visible && !prevProps.visible) {
      setTimeout(() => {
        this.input.focus();
      });   
    }
  }

  handleCreateEpic =(e) => {
    const {
      form, onOk, epicType, defaultPriority, 
    } = this.props;   
    const defaultPriorityId = defaultPriority ? defaultPriority.id : '';
    e.preventDefault();
    form.validateFieldsAndScroll((err, value) => {
      if (!err) {        
        const data = {
          projectId: AppState.currentMenuType.id,
          epicName: value.name.trim(),
          summary: value.summary.trim(),
          typeCode: 'issue_epic',
          issueTypeId: epicType && epicType.id,
          priorityCode: `priority-${defaultPriorityId}`,
          priorityId: defaultPriorityId,
        };
        this.setState({
          loading: true,
        });
        issueApi.create(data)
          .then((res) => {
            const dto = {
              schemeCode: 'agile_issue',
              context: res.typeCode,
              pageCode: 'agile_issue_create',
            };
            fieldApi.quickCreateDefault(res.issueId, dto);
            this.setState({
              loading: false,
            });
            onOk(res);
          })
          .catch((error) => {
            this.setState({
              loading: false,
            });
          });
      }
    });
  }

  checkEpicNameRepeat = (rule, value, callback) => {
    if (value && value.trim()) {
      epicApi.checkName(value)
        .then((res) => {
          if (res) {
            callback('史诗名称重复');
          } else {
            callback();
          }
        });
    } else {
      callback();
    }
  };

  render() {
    const { loading } = this.state;
    const {
      form, visible, onCancel,
    } = this.props;
    const { getFieldDecorator } = form;

    return (
      <Sidebar
        title="创建史诗"       
        visible={visible}
        okText="创建"
        cancelText="取消"
        onCancel={() => {
          form.resetFields();
          onCancel();
        }}
        destroyOnClose
        confirmLoading={loading}
        onOk={this.handleCreateEpic}
        width={380}
      >        
        <Form>
          <FormItem>
            {getFieldDecorator('name', {
              rules: [{
                required: true,
                message: '史诗名称不能为空',
                whitespace: true,
              }, {
                validator: this.checkEpicNameRepeat,
              }],
            })(
              <Input ref={(input) => { this.input = input; }} label="史诗名称" maxLength={20} />,
            )}
          </FormItem>
          <FormItem>
            {getFieldDecorator('summary', {
              rules: [{
                required: true,
                message: '概要不能为空',
                whitespace: true,
              }],
            })(
              <TextArea autosize label="概要" maxLength={44} />,
            )}
          </FormItem>
        </Form>        
      </Sidebar>
    );
  }
}
const CreateEpicModalContainer = observer(({ ...props }) => {
  const { createEpicModalVisible } = StoryMapStore;
  return (
    <CreateEpicModal
      visible={createEpicModalVisible}
      epicType={StoryMapStore.getEpicType}
      defaultPriority={StoryMapStore.getDefaultPriority}
      onCancel={() => {
        StoryMapStore.setCreateEpicModalVisible(false);
      }}
      {...props}
    />
  );
});
export default Form.create()(CreateEpicModalContainer);
