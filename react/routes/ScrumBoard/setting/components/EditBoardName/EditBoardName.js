import React, { Component } from 'react';
import {
  stores, Content, Choerodon, 
} from '@choerodon/boot';
import { withRouter } from 'react-router-dom';
import {
  Form, Input, Button, Spin,
} from 'choerodon-ui';
import ScrumBoardStore from '@/stores/project/scrumBoard/ScrumBoardStore';
import { boardApi } from '@/api';

const { AppState } = stores;
const FormItem = Form.Item;

class EditBoardName extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      initialBoardName: '',
      boardName: '',
      lastBoardName: '',
    };
  }

  componentDidMount() {
    const initialBoardName = ScrumBoardStore.getBoardList.get(ScrumBoardStore.getSelectedBoard).name;
    const { saveRef } = this.props;
    saveRef(this.boardName);
    this.setState({
      initialBoardName,
      lastBoardName: initialBoardName,
    });
  }

  checkBoardNameRepeat = (rule, value, callback) => {
    const { initialBoardName } = this.state;
    const proId = AppState.currentMenuType.id;
    if (initialBoardName === value) {
      callback();
    }
    boardApi.checkName(value).then((res) => {
      if (res) {
        callback('看板名称重复');
      } else {
        this.setState(
          {
            boardName: value,
          },
        );
        callback();
      }
    });
  };

  handleUpdateBoardName = () => {
    const { boardName } = this.state;
    const { form, history } = this.props;
    const currentEditBoard = ScrumBoardStore.getBoardList.get(ScrumBoardStore.getSelectedBoard);
    const { objectVersionNumber, boardId, projectId } = currentEditBoard;
    const data = {
      objectVersionNumber,
      boardId,
      name: boardName,
      projectId,
    };

    form.validateFields((err, value, modify) => {
      if (!err && modify) {
        this.setState({
          loading: true,
          lastBoardName: value.boardName,
        });
        boardApi.update(data.boardId, data).then((res) => {
          this.setState({
            loading: false,
          });
          ScrumBoardStore.setBoardList(ScrumBoardStore.getSelectedBoard, res);
          Choerodon.prompt('保存成功');
          // history.push(`/agile/scrumboard?type=project&id=${data.projectId}&name=${encodeURIComponent(AppState.currentMenuType.name)}&organizationId=${AppState.currentMenuType.organizationId}`);
        });
      }
    });
  }

  render() {
    const { form: { getFieldDecorator, setFieldsValue } } = this.props;
    const {
      initialBoardName, loading, lastBoardName,
    } = this.state;
    const { editBoardNameDisabled } = this.props;
    return (
      <Content
        style={{
          padding: 0,
          height: '100%',
        }}
      >
        <Spin spinning={loading}>
          {
            editBoardNameDisabled ? (
              <Input
                style={{ width: 512, marginTop: 5 }}
                label="看板名称"
                maxLength={10}
                value={initialBoardName}
                disabled
              />
            ) : (
              <div>
                <Form layout="vertical">
                  <FormItem label="boardName" style={{ width: 512 }}>
                    {getFieldDecorator('boardName', {
                      rules: [{ required: true, message: '看板名称必填' }, {
                        validator: this.checkBoardNameRepeat,
                      }],
                      initialValue: initialBoardName,
                    })(
                      <Input
                        label="看板名称"
                        ref={(ref) => { this.boardName = ref; }}
                        maxLength={10}
                      />,
                    )}
                  </FormItem>
                </Form>
                <div style={{ padding: '12px 0', borderTop: '1px solid rgba(0, 0, 0, 0.12)' }}>
                  <Button
                    type="primary"
                    funcType="raised"
                    loading={loading}
                    onClick={this.handleUpdateBoardName}
                  >
                    保存
                  </Button>
                  <Button
                    funcType="raised"
                    style={{ marginLeft: 12 }}
                    onClick={() => {
                      setFieldsValue({
                        boardName: lastBoardName,
                      });
                      this.setState({
                        boardName: initialBoardName,
                      });
                    }}
                  >
                    取消
                  </Button>
                </div>
              </div>
            )
          }

        </Spin>

      </Content>
    );
  }
}
export default withRouter(Form.create()(EditBoardName));
