import React, { Component } from 'react';
import { observer } from 'mobx-react';
import {
  Modal, Form, Input,
} from 'choerodon-ui';
import { stores } from '@choerodon/boot';
import {
  epicApi, issueApi, fileApi, fieldApi, 
} from '@/api';
import BacklogStore from '../../../../stores/project/backlog/BacklogStore';

const { AppState } = stores;
const { Sidebar } = Modal;
const FormItem = Form.Item;
const { TextArea } = Input;

@Form.create({})
@observer
class CreateEpic extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
    };
  }

  /**
   *
   * 创建史诗
   * @param {*} e
   * @memberof CreateEpic
   */
  handleCreateEpic =(e) => {
    const {
      form, onCancel, refresh,
    } = this.props;
    const issueTypes = BacklogStore.getIssueTypes || [];
    const defaultPriorityId = BacklogStore.getDefaultPriority ? BacklogStore.getDefaultPriority.id : '';
    e.preventDefault();
    form.validateFieldsAndScroll((err, value) => {
      if (!err) {
        const epicType = issueTypes.find(t => t.typeCode === 'issue_epic');
        const req = {
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
        issueApi.create(req).then((res) => {
          const dto = {
            schemeCode: 'agile_issue',
            context: res.typeCode,
            pageCode: 'agile_issue_create',
          };
          fieldApi.quickCreateDefault(res.issueId, dto);
          this.setState({
            loading: false,
          });
          form.resetFields();
          refresh();
          onCancel();
        }).catch((error) => {
          this.setState({
            loading: false,
          });
        });
      }
    });
  };

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
    const {
      form, onCancel, visible, store,
    } = this.props;
    const issueTypes = BacklogStore.getIssueTypes || [];
    const epicType = issueTypes.find(t => t.typeCode === 'issue_epic');
    const { loading } = this.state;
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
              <Input label="史诗名称" maxLength={20} />,
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
              <TextArea autosize={{ minRows: 3, maxRows: 10 }} label="概要" maxLength={44} />,
            )}
          </FormItem>
        </Form>       
      </Sidebar>
    );
  }
}

export default CreateEpic;
