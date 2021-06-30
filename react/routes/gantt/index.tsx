/* eslint-disable no-param-reassign */
import React, {
  useState, useEffect, useCallback, useMemo,
} from 'react';
// eslint-disable-next-line camelcase
import { unstable_batchedUpdates } from 'react-dom';
import { Tooltip, Icon } from 'choerodon-ui/pro';
import { observer } from 'mobx-react-lite';
import { find } from 'lodash';
import produce from 'immer';
import dayjs from 'dayjs';
import weekday from 'dayjs/plugin/weekday';
import classNames from 'classnames';
import { usePersistFn } from 'ahooks';
import moment from 'moment';
import {
  Page, Header, Content, Breadcrumb, HeaderButtons,
} from '@choerodon/boot';
import GanttComponent, { GanttProps, Gantt, GanttRef } from 'react-gantt-component';
import 'react-gantt-component/dist/react-gantt-component.cjs.production.min.css';
import { ganttApi, issueApi, workCalendarApi } from '@/api';
import { localPageCacheStore } from '@/stores/common/LocalPageCacheStore';
import TypeTag from '@/components/TypeTag';
import Loading from '@/components/Loading';
import SelectSprint from '@/components/select/select-sprint';
import { FlatSelect } from '@choerodon/components';
import useFullScreen from '@/common/useFullScreen';
import { ILocalField } from '@/components/issue-search/store';
import { getSystemFields } from '@/stores/project/issue/IssueStore';
import { useIssueSearchStore } from '@/components/issue-search';
import FilterManage from '@/components/FilterManage';
import { Issue, User } from '@/common/types';
import isHoliday from '@/utils/holiday';
import { transformFilter } from '@/routes/Issue/stores/utils';
import Search from './components/search';
import GanttBar from './components/gantt-bar';
import GanttGroupBar from './components/gantt-group-bar';
import IssueDetail from './components/issue-detail';
import CreateIssue from './components/create-issue';
import Context from './context';
import GanttStore from './store';
import GanttOperation from './components/gantt-operation';
import './index.less';

dayjs.extend(weekday);
const typeOptions = [{
  value: 'task',
  label: '按任务查看',
}, {
  value: 'assignee',
  label: '按经办人查看',
}] as const;
const typeValues = typeOptions.map((t) => t.value);
type TypeValue = (typeof typeValues)[number];
const renderTooltip = (user: User) => {
  const {
    loginName, realName, email, ldap,
  } = user || {};
  return ldap ? `${realName}(${loginName})` : `${realName}(${email})`;
};
const { Option } = FlatSelect;
const tableColumns: GanttProps<Issue>['columns'] = [{
  flex: 2,
  minWidth: 200,
  name: 'summary',
  label: '名称',
  render: (record) => (
    <Tooltip title={record.summary}>
      {!record.group ? (
        <span style={{ cursor: 'pointer', color: 'var(--table-click-color)' }}>
          <TypeTag iconSize={22} data={record.issueTypeVO} style={{ marginRight: 5 }} />
          <span style={{ verticalAlign: 'middle' }}>{record.summary}</span>
        </span>
      ) : <span style={{ color: 'var(--table-click-color)' }}>{record.summary}</span>}
    </Tooltip>
  ),
},
{
  width: 80,
  minWidth: 80,
  name: 'assignee',
  label: '经办人',
  render: (record) => (
    <Tooltip title={renderTooltip(record.assignee)}>
      <span>{record.assignee?.realName}</span>
    </Tooltip>
  ),
},
{
  flex: 1,
  minWidth: 100,
  name: 'estimatedStartTime',
  label: '预计开始',
  render: (record) => record.estimatedStartTime && <Tooltip title={record.estimatedStartTime}><span>{dayjs(record.estimatedStartTime).format('YYYY-MM-DD')}</span></Tooltip>,
},
{
  flex: 1,
  minWidth: 100,
  name: 'estimatedEndTime',
  label: '预计结束',
  render: (record) => record.estimatedEndTime && <Tooltip title={record.estimatedEndTime}><span>{dayjs(record.estimatedEndTime).format('YYYY-MM-DD')}</span></Tooltip>,
}];
const GanttPage: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [type, setType] = useState<TypeValue>(localPageCacheStore.getItem('gantt.search.type') ?? typeValues[0]);
  const [columns, setColumns] = useState<Gantt.Column[]>([]);
  const [workCalendar, setWorkCalendar] = useState<any>();
  const [projectWorkCalendar, setProjectWorkCalendar] = useState<any>();
  const [filterManageVisible, setFilterManageVisible] = useState<boolean>();
  const [loading, setLoading] = useState(false);
  const issueSearchStore = useIssueSearchStore({
    getSystemFields: () => getSystemFields().filter((item) => item.code !== 'sprint') as ILocalField[],
    transformFilter,
  });
  const store = useMemo(() => new GanttStore(), []);
  const { sprintIds } = store;
  const [isFullScreen, toggleFullScreen] = useFullScreen(() => document.body, () => { }, 'c7n-gantt-fullScreen');
  const loadData = useCallback(() => {
    (async () => {
      const year = dayjs().year();
      const filter = issueSearchStore.getCustomFieldFilters();
      if (sprintIds === null) {
        return;
      }
      filter.otherArgs.sprint = sprintIds;
      setLoading(true);
      const [workCalendarRes, projectWorkCalendarRes, res] = await Promise.all([
        workCalendarApi.getWorkSetting(year),
        workCalendarApi.getYearCalendar(year),
        type === 'task' ? ganttApi.loadByTask(filter) : ganttApi.loadByUser(filter),
      ]);
      // setColumns(headers.map((h: any) => ({
      //   width: 100,
      //   name: h.fieldCode,
      //   label: h.name,
      // })));
      unstable_batchedUpdates(() => {
        setWorkCalendar(workCalendarRes);
        setProjectWorkCalendar(projectWorkCalendarRes);
        setColumns(tableColumns);
        setData(res);
        setLoading(false);
      });
    })();
  }, [issueSearchStore, sprintIds, type]);
  useEffect(() => {
    loadData();
  }, [issueSearchStore, loadData]);
  const handleUpdate = useCallback<GanttProps<Issue>['onUpdate']>(async (issue, startDate, endDate) => {
    try {
      await issueApi.update({
        issueId: issue.issueId,
        objectVersionNumber: issue.objectVersionNumber,
        estimatedStartTime: startDate,
        estimatedEndTime: endDate,
      });
      issue.objectVersionNumber += 1;
      issue.estimatedStartTime = startDate;
      issue.estimatedEndTime = endDate;
      return true;
    } catch (error) {
      return false;
    }
  }, []);
  const handleSprintChange = useCallback((value: string[]) => {
    store.setSprintIds(value);
  }, [store]);
  const afterSprintLoad = useCallback((sprints) => {
    if (!sprintIds) {
      const cachedSprintId = localPageCacheStore.getItem('gantt.search.sprints');
      if (cachedSprintId) {
        store.setSprintIds(cachedSprintId);
      } else {
        const currentSprint = find(sprints, { statusCode: 'started' });
        if (currentSprint) {
          store.setSprintIds([currentSprint.sprintId]);
        } else {
          store.setSprintIds([sprints[0]?.sprintId || '0']);
        }
      }
    }
  }, [sprintIds, store]);
  const handleTypeChange = useCallback((newType) => {
    setType(newType);
    localPageCacheStore.setItem('gantt.search.type', newType);
  }, []);
  const isRestDay = useCallback((date: string) => isHoliday({
    sprintSetting: projectWorkCalendar,
    orgWorkCalendar: workCalendar,
  }, moment(date)), [projectWorkCalendar, workCalendar]);
  const handleClickFilterManage = () => {
    setFilterManageVisible(true);
  };
  const { unit } = store;
  const onRow: GanttProps<Issue>['onRow'] = useMemo(() => ({
    onClick: (issue) => {
      store.setIssueId(issue.issueId);
    },
  }), [store]);
  const getExpandIcon = useCallback(({ level, collapsed, onClick }) => (
    <div
      role="none"
      onClick={onClick}
      className={classNames('c7n-gantt-expand-icon', {
        'c7n-gantt-expand-icon-expanded': !collapsed,
      })}
    >
      <Icon type="navigate_next" />
    </div>
  ), []);
  const renderBar: GanttProps['renderBar'] = useCallback((bar, { width, height }) => (
    <GanttBar
      type={type}
      bar={bar}
      width={width}
      height={height}
      onClick={onRow.onClick}
    />
  ), [onRow.onClick, type]);
  const renderGroupBar: GanttProps['renderBar'] = useCallback((bar, { width, height }) => (
    <GanttGroupBar
      bar={bar}
      width={width}
      height={height}
    />
  ), []);
  const renderInvalidBar: GanttProps['renderInvalidBar'] = useCallback((element, barInfo) => (
    <Tooltip
      // @ts-ignore
      getPopupContainer={(t) => document.getElementsByClassName('gantt-chart')[0] as HTMLElement}
      hidden={barInfo.stepGesture === 'moving'}
      placement="top"
      title="点击并拖动以设置预计开始、结束时间。"
    >
      {element}
    </Tooltip>
  ), []);

  const renderBarThumb: GanttProps['renderBarThumb'] = useCallback((record, t) => (
    <div
      role="none"
      className="c7n-gantt-thumb-icon"
    >
      {t === 'left' ? <Icon type="navigate_before" /> : <Icon type="navigate_next" />}
    </div>
  ), []);
  const normalizeIssue = (issue:Issue, source:any = {}) => Object.assign(source, {
    estimatedEndTime: issue.estimatedEndTime,
    estimatedStartTime: issue.estimatedStartTime,
    issueTypeVO: issue.issueTypeVO,
    objectVersionNumber: issue.objectVersionNumber,
    statusVO: issue.statusVO,
    summary: issue.summary,
  });
  const handleIssueUpdate = usePersistFn((issue:Issue) => {
    setData(produce(data, (draft) => {
      const target = find(draft, { issueId: issue.issueId });
      if (target) {
        // 更新属性
        normalizeIssue(issue, target);
      }
    }));
  });

  const handleAddIssue = usePersistFn((issue:Issue) => {
    setData(produce(data, (draft) => {
      const target = find(draft, { issueId: issue.issueId });
      if (target) {
        // 更新属性
        Object.assign(target, {
          estimatedEndTime: issue.estimatedEndTime,
          estimatedStartTime: issue.estimatedStartTime,
          issueTypeVO: issue.issueTypeVO,
          objectVersionNumber: issue.objectVersionNumber,
          statusVO: issue.statusVO,
          summary: issue.summary,
        });
      }
    }));
  });
  const handleCreateIssue = usePersistFn((issue:Issue) => {
    const parentIssueId = issue.parentIssueId ?? issue.relateIssueId;
    if (parentIssueId) {
      setData(produce(data, (draft) => {
        const parent = find(draft, { issueId: parentIssueId });
        if (parent) {
          parent.children.unshift(normalizeIssue(issue));
        }
      }));
    } else {
      setData(produce(data, (draft) => {
        draft.unshift(normalizeIssue(issue));
      }));
    }
  });
  return (
    <Page>
      <Header>
        <SelectSprint
          flat
          placeholder="冲刺"
          value={sprintIds}
          multiple
          onChange={handleSprintChange}
          clearButton={false}
          afterLoad={afterSprintLoad}
          hasUnassign
          style={{ marginRight: 16 }}
          searchable={false}
        />
        <FlatSelect value={type} onChange={handleTypeChange} clearButton={false} style={{ marginRight: 8 }}>
          {typeOptions.map((o) => (
            <Option value={o.value}>
              {o.label}
            </Option>
          ))}
        </FlatSelect>
        <HeaderButtons
          showClassName={false}
          items={[
            {
              name: '创建问题',
              icon: 'playlist_add',
              display: true,
              handler: () => {
                store.setCreateIssueVisible(true);
              },
            },
            {
              name: '个人筛选',
              icon: 'settings-o',
              display: true,
              handler: handleClickFilterManage,
            },
            {
              icon: isFullScreen ? 'fullscreen_exit' : 'zoom_out_map',
              iconOnly: true,
              display: true,
              handler: () => {
                // @ts-ignore
                toggleFullScreen();
              },
              tooltipsConfig: {
                title: isFullScreen ? '退出全屏' : '全屏',
              },
            },
            {
              display: true,
              icon: 'refresh',
              // funcType: 'flat',
              handler: loadData,
            },
          ]}
        />
      </Header>
      <Breadcrumb />
      <Content
        className="c7n-gantt-content"
        style={{
          borderTop: '1px solid rgb(216, 216, 216)',
          display: 'flex',
          paddingTop: 7,
          flexDirection: 'column',
          paddingBottom: 0,
        }}
      >
        <Context.Provider value={{ store }}>
          <div style={{ display: 'flex' }}>
            <Search issueSearchStore={issueSearchStore} loadData={loadData} />
            <GanttOperation />
          </div>
          <Loading loading={loading} />
          {columns.length > 0 && workCalendar && (
            <GanttComponent
              innerRef={store.ganttRef as React.MutableRefObject<GanttRef>}
              data={data}
              columns={columns}
              onUpdate={handleUpdate}
              startDateKey="estimatedStartTime"
              endDateKey="estimatedEndTime"
              isRestDay={isRestDay}
              showBackToday={false}
              showUnitSwitch={false}
              unit={unit}
              onRow={onRow}
              onBarClick={onRow.onClick}
              tableIndent={20}
              expandIcon={getExpandIcon}
              renderBar={renderBar}
              renderInvalidBar={renderInvalidBar}
              renderGroupBar={renderGroupBar}
              renderBarThumb={renderBarThumb}
              tableCollapseAble={false}
              scrollTop={{
                right: -4,
                bottom: 8,
              }}
              rowHeight={34}
            />
          )}
          <IssueDetail refresh={loadData} onUpdate={handleIssueUpdate} />
          <CreateIssue onCreate={handleCreateIssue} />
          <FilterManage
            visible={filterManageVisible!}
            setVisible={setFilterManageVisible}
            issueSearchStore={issueSearchStore}
          />
        </Context.Provider>
      </Content>
    </Page>
  );
};
export default observer(GanttPage);
