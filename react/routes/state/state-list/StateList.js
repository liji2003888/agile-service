/* eslint-disable react/jsx-no-bind */
import React, {
  useState, useEffect, useContext, useCallback,
} from 'react';
import { observer } from 'mobx-react-lite';
import {
  Table, Modal, Form, Select, Input, Tooltip, Menu,
} from 'choerodon-ui';
import { Button as ButtonPro } from 'choerodon-ui/pro';
import { FormattedMessage } from 'react-intl';
import {
  Content, Header, TabPage as Page, Breadcrumb, Choerodon, useTheme,
} from '@choerodon/boot';
import { HeaderButtons } from '@choerodon/master';
import TableDropMenu from '@/components/table-drop-menu';
import { getStageMap, getStageList } from '@/utils/stateMachine';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import { statusApi } from '@/api';
import Store from './stores';
import './StateList.less';
import openStateModal from './StateModal';

const backlogStates = ['backlog_pending_approval', 'backlog_rejected', 'backlog_create', 'backlog_planning', 'backlog_processing', 'backlog_developed', 'backlog_publish'];
const { Sidebar } = Modal;
const FormItem = Form.Item;
const { TextArea } = Input;
const { Option } = Select;
const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 100 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 26 },
  },
};
const prefixCls = 'issue-state';

const stageMap = getStageMap();
const stageList = getStageList();

function StateList(props) {
  const [theme] = useTheme();
  const context = useContext(Store);
  const { AppState, stateStore, intl: { formatMessage } } = context;
  const {
    organizationId: orgId,
  } = AppState.currentMenuType;
  //  这三个放到一起管理
  const [deleteVisible, setDeleteVisible] = useState(false);
  const [deleteId, setDeleteId] = useState('');
  const [deleteName, setDeleteName] = useState('');

  const [statesList, setStatesList] = useState({
    list: [],
    total: 0,
  });
  const [initialTotal, setInitialTotal] = useState(0);

  const [pagination, setPagination] = useState({
    page: 1,
    pageSize: 10,
    total: 0,
  });
  const [tableParam, setTableParam] = useState({
    sorter: undefined,
    param: {},
    page: 1,
    pageSize: 10,
  });

  const confirmDelete = (record) => {
    setDeleteId(record.id);
    setDeleteName(record.name);
    setDeleteVisible(true);
  };

  const handleCancel = () => {
    setDeleteId('');
    setDeleteVisible(false);
  };

  const loadState = ({
    page = 1, size = 10, sort = { field: 'id', order: 'desc' }, param = {}, isSetInitialTotal = false,
  }) => {
    stateStore.loadStateList(orgId, page, size, sort, param, isSetInitialTotal).then((data) => {
      setStatesList({
        list: data.list,
        total: data.total,
      });
      setPagination({
        page,
        pageSize: size,
        total: data.total,
      });
      if (isSetInitialTotal) {
        setInitialTotal(data.total);
      }
    });
  };

  const handleDelete = () => {
    statusApi.delete(deleteId).then((data) => {
      if (data && data.failed) {
        Choerodon.prompt(data.message);
      } else {
        loadState({
          page: pagination.page, size: pagination.pageSize, sort: tableParam.sorter, param: tableParam.param,
        });
        setDeleteId('');
        setDeleteVisible(false);
      }
    });
  };

  const tableChange = (newPagination, filters, sorter, param) => {
    const sort = {};
    if (sorter.column) {
      const { field, order } = sorter;
      sort[field] = order;
    }
    let searchParam = {};
    if (filters && filters.name && filters.name.length) {
      searchParam = {
        ...searchParam,
        name: filters.name[0],
      };
    }
    if (filters && filters.description && filters.description.length) {
      searchParam = {
        ...searchParam,
        description: filters.description[0],
      };
    }
    if (filters && filters.type && filters.type.length) {
      searchParam = {
        ...searchParam,
        type: filters.type[0],
      };
    }
    if (param && param.length) {
      searchParam = {
        ...searchParam,
        param: param.toString(),
      };
    }
    setTableParam({
      page: newPagination.current,
      pageSize: newPagination.pageSize,
      sorter: sorter.column ? sorter : undefined,
      param: searchParam,
    });
    loadState({
      page: newPagination.current,
      size: newPagination.pageSize,
      sort: sorter.column ? sorter : undefined,
      param: searchParam,
    });
  };

  const getColumn = () => ([{
    title: <FormattedMessage id="state.name" />,
    dataIndex: 'name',
    key: 'name',
    filters: [],
    render: (text, record) => {
      const menu = (
        <Menu onClick={confirmDelete.bind(this, record)}>
          <Menu.Item key="del">
            <Tooltip placement="top" title={<FormattedMessage id="delete" />}>
              <span>
                删除
              </span>
            </Tooltip>
          </Menu.Item>
        </Menu>
      );
      return (
        <TableDropMenu
          oldMenuData={menu}
          showMenu={!(record.code || (record.stateMachineInfoList && record.stateMachineInfoList.length))}
          menuData={[{
            action: () => {
              openStateModal({
                onOk: handleOnOk, statusId: record.id, name: record.name, disabledEditName: backlogStates.includes(record.code),
              });
            },
            text: '编辑',
          }]}
          text={text}
        />
      );
    },
  },
  {
    title: <FormattedMessage id="state.des" />,
    dataIndex: 'description',
    key: 'description',
    filters: [],
    className: 'issue-table-ellipsis',
  },
  {
    title: <FormattedMessage id="state.stage" />,
    dataIndex: 'type',
    key: 'type',
    filters: stageList.filter((s) => s.code !== 'none').map((s) => ({ text: s.name, value: s.code })),
    render: (record) => (
      <div>
        <div className="issue-state-block" style={{ backgroundColor: stageMap[record]?.colour }} />
        <span style={{ verticalAlign: 'middle' }}>{stageMap[record]?.name}</span>
      </div>
    ),
  }]);

  useEffect(() => {
    loadState({
      page: undefined,
      size: undefined,
      sort: undefined,
      param: undefined,
      isSetInitialTotal: true,
    });
  }, []);

  const handleOnOk = useCallback(() => {
    loadState({
      page: pagination.page, size: pagination.pageSize, sort: tableParam.sorter, param: tableParam.param,
    });
  }, [loadState, pagination.page, pagination.pageSize, tableParam.param, tableParam.sorter]);

  function render() {
    const pageInfo = {
      current: pagination.page,
      pageSize: pagination.pageSize,
      total: pagination.total,
    };

    return (
      <Page>
        <Header title={<FormattedMessage id="state.title" />}>
          <HeaderButtons items={[
            {
              name: formatMessage({ id: 'state.create' }),
              icon: 'playlist_add',
              display: true,
              disabled: !initialTotal,
              handler: () => { openStateModal({ onOk: handleOnOk }); },
              tooltipsConfig: {
                hidden: initialTotal,
                title: '请创建项目后再创建状态机',
              },
            },
          ]}
          />
        </Header>
        <Breadcrumb />
        <Content className="issue-state-content" style={theme === 'theme4' ? undefined : { paddingTop: 0 }}>
          <Table
            dataSource={statesList.list}
            columns={getColumn()}
            filterBarPlaceholder="过滤表"
            rowKey={(record) => record.id}
            loading={stateStore.getIsLoading}
            pagination={pageInfo}
            onChange={tableChange}
            className="issue-table"
          />
        </Content>
        <Modal
          title={<FormattedMessage id="state.delete" />}
          visible={deleteVisible}
          onOk={handleDelete}
          onCancel={handleCancel}
        >
          <p className={`${prefixCls}-del-content`}>
            <FormattedMessage id="state.delete" />
            <span>:</span>
            <span className={`${prefixCls}-del-content-name`}>{deleteName}</span>
          </p>
          <p className={`${prefixCls}-del-tip`}>
            <FormattedMessage id="state.delete.tip" />
          </p>
        </Modal>
      </Page>
    );
  }
  return render();
}
// withRouter 原先在这包裹
export default Form.create({})(observer(StateList));
