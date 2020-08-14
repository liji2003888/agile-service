import React, {
  useContext, useRef, useEffect, useState,
} from 'react';
import { observer } from 'mobx-react-lite';
import { withRouter } from 'react-router-dom';
import {
  Header, Content, Page, Breadcrumb, Choerodon,
} from '@choerodon/boot';
import { Button } from 'choerodon-ui';
import queryString from 'querystring';
import { map } from 'lodash';
import CreateIssue from '@/components/CreateIssue';
import { projectApi } from '@/api/Project';
import { issueApi } from '@/api';
import IssueStore from '../../stores/project/issue/IssueStore';
import Store, { StoreProvider } from './stores';
import Search from './components/search';
import FilterManage from './components/FilterManage';
import SaveFilterModal from './components/SaveFilterModal';
import ExportIssue from './components/ExportIssue';
import IssueDetail from './components/issue-detail';
import ImportIssue from './components/ImportIssue';
import IssueTable from './components/issue-table';
import CollapseAll from './components/issue-table/CollapseAll';
import Modal from './components/Modal';
import './index.less';

const Issue = withRouter(observer(() => {
  const {
    dataSet, projectId,
  } = useContext(Store);
  const [urlFilter, setUrlFilter] = useState(null);
  const importRef = useRef();
  const tableRef = useRef();
  /**
   * 默认此次操作不是删除操作
   * 防止删除此页一条数据时页时停留当前页时出现无数据清空
   * @param {Boolean} isDelete  用于标记是否为删除操作
   */
  const refresh = (isDelete = false) => dataSet.query(
    isDelete
    && dataSet.length === 1
    && dataSet.totalCount > 1
      ? dataSet.currentPage - 1
      : dataSet.currentPage,
  );

  const initFilter = async () => {
    const {
      paramChoose, paramCurrentVersion, paramCurrentSprint, paramId,
      paramType, paramIssueId, paramName, paramOpenIssueId,
      // eslint-disable-next-line no-restricted-globals
    } = queryString.parse(location.href);
    let prefix = '';
    if (paramChoose) {
      if (paramChoose === 'version' && paramCurrentVersion) {
        IssueStore.handleFilterChange(paramChoose, [paramCurrentVersion]);
        prefix = '版本';
      }
      if (paramChoose === 'sprint' && paramCurrentSprint) {
        IssueStore.handleFilterChange(paramChoose, [paramCurrentSprint]);
        prefix = '冲刺';
      }
    }
    const prefixs = {
      assigneeId: '经办人',
      typeCode: '类型',
      priority: '优先级',
      statusId: '状态',
      fixVersion: '版本',
      version: '版本',
      component: '模块',
      sprint: '冲刺',
      epic: '史诗',
      label: '标签',
    };
    if (paramType) {
      prefix = prefixs[paramType];
      IssueStore.handleFilterChange(paramType, [paramId]);
    }
    setUrlFilter(`${prefix ? `${prefix}:` : ''}${paramName || ''}`);
    // this.paramName = decodeURI(paramName);
    // 单个任务跳转 => otherArgs 设置 issueId，将任务设定为展开模式
    if (paramIssueId) {
      let id = paramOpenIssueId || paramIssueId;
      // 数字，说明没加密
      // eslint-disable-next-line no-restricted-globals
      if (!isNaN(id)) {
        try {
          id = await issueApi.encryptIssueId(id);
        } catch (error) {
          Choerodon.prompt(error.message, 'error');
        }
      }
      IssueStore.handleFilterChange('issueIds', [id]);
      IssueStore.handleFilterChange('contents', [`${IssueStore.getProjectInfo.projectCode}-${paramName.split('-')[paramName.split('-').length - 1]}`]);
      IssueStore.setClickedRow({
        selectedIssue: {
          issueId: id,
        },
        expand: true,
      });
    } else {
      await IssueStore.query();
    }
  };
  const handleClear = () => {
    setUrlFilter(null);
  };
  const getProjectInfo = () => {
    projectApi.loadInfo().then((res) => {
      IssueStore.setProjectInfo(res);
      initFilter();
    });
  };
  useEffect(() => {
    getProjectInfo();
    IssueStore.initChosenFields();
    return () => {
      IssueStore.setClickedRow({ selectedIssue: {}, expand: false });
      IssueStore.setFilterListVisible(false);
      Modal.close('modal');
    };
  }, []);

  const handleCreateIssue = (issue) => {
    IssueStore.createQuestion(false);
    dataSet.query();
  };

  const handleClickFilterManage = () => {
    const editFilterInfo = IssueStore.getEditFilterInfo;
    const filterListVisible = IssueStore.getFilterListVisible;
    IssueStore.setSaveFilterVisible(false);
    IssueStore.setFilterListVisible(!filterListVisible);
    IssueStore.setEditFilterInfo(map(editFilterInfo, (item) => Object.assign(item, {
      isEditing:
      false,
    })));
  };
  return (
    <Page
      className="c7nagile-issue"
      service={[
        'choerodon.code.project.cooperation.work-list.ps.issue',
      ]}
    >
      <Header
        title="问题管理"
      >
        <Button
          className="leftBtn"
          funcType="flat"
          icon="playlist_add"
          onClick={() => {
            IssueStore.createQuestion(true);
          }}
        >
          创建问题
        </Button>
        <Button icon="archive" funcType="flat" onClick={() => importRef.current.open()}>
          导入问题
        </Button>
        <Button
          className="leftBtn"
          icon="unarchive"
          funcType="flat"
          onClick={() => {
            IssueStore.setExportModalVisible(true);
          }}
        >
          导出问题
        </Button>
        <Button onClick={handleClickFilterManage} icon="settings">筛选管理</Button>
        <CollapseAll tableRef={tableRef} />
      </Header>
      <Breadcrumb />
      <Content style={{ paddingTop: 0 }} className="c7nagile-issue-content">
        <Search urlFilter={urlFilter} onClear={handleClear} />
        <IssueTable tableRef={tableRef} onCreateIssue={handleCreateIssue} />
        <SaveFilterModal />
        <FilterManage />
        <ExportIssue dataSet={dataSet} tableRef={tableRef} onCreateIssue={handleCreateIssue} />
        {IssueStore.getCreateQuestion && (
          <CreateIssue
            visible={IssueStore.getCreateQuestion}
            onCancel={() => { IssueStore.createQuestion(false); }}
            onOk={handleCreateIssue}
          />
        )}
        <IssueDetail
          issueRefresh={refresh}
          dataSet={dataSet}
        />
        <ImportIssue ref={importRef} onFinish={refresh} />
      </Content>
    </Page>
  );
}));

export default (props) => (
  <StoreProvider {...props}>
    <Issue />
  </StoreProvider>
);
