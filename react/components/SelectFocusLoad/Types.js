/* eslint-disable camelcase */
import React from 'react';
import { Select } from 'choerodon-ui';
import { find } from 'lodash';
import {
  userApi, componentApi, issueApi, epicApi, versionApi, issueTypeApi, commonApi, issueLabelApi, priorityApi, statusApi,
} from '@/api';

import { issueLinkTypeApi } from '@/api/IssueLinkType';
import { featureApi, piApi, sprintApi } from '@/api';
import { Tooltip } from 'choerodon-ui/pro';
import UserHead from '../UserHead';
import TypeTag from '../TypeTag';
import StatusTag from '../StatusTag';
import { IsInProgramStore } from '../../exports';
// 增加 typeof 避免选项中 加载更多 影响 
const filterOption = (input, option) => option.props.children && typeof (option.props.children) === 'string' && option.props.children.toLowerCase().indexOf(
  input.toLowerCase(),
) >= 0;
const filterOptionByName = (input, option) => option.props.name && typeof (option.props.name) === 'string' && option.props.name.toLowerCase().indexOf(
  input.toLowerCase(),
) >= 0;
export function transform(links) {
  // split active and passive
  const active = links.map(link => ({
    name: link.outWard,
    isIn: false,
    linkTypeId: link.linkTypeId,
  }));
  const passive = [];
  links.forEach((link) => {
    if (link.inWard !== link.outWard) {
      passive.push({
        name: link.inWard,
        isIn: true,
        linkTypeId: link.linkTypeId,
      });
    }
  });

  return active.concat(passive);
}
const { Option, OptGroup } = Select;
const issue_type_program = {
  props: {
    filterOption,
  },
  request: () => new Promise(resolve => issueTypeApi.loadAllWithStateMachineId('program').then((issueTypes) => {
    resolve(issueTypes);
  })),
  render: issueType => (
    <Option
      key={issueType.id}
      value={issueType.id}
      name={issueType.name}
    >
      <div style={{ display: 'inline-flex', alignItems: 'center', padding: '2px' }}>
        <TypeTag
          data={issueType}
          showName
        />
      </div>
    </Option>
  ),
};
export default {
  user: {
    request: ({ filter, page }) => userApi.getAllInProject(filter, page).then(UserData => ({ ...UserData, list: UserData.list.filter(user => user.enabled) })),
    render: user => (
      <Option key={user.id} value={user.id}>
        <div style={{
          display: 'inline-flex', alignItems: 'center', padding: 2, verticalAlign: 'sub',
        }}
        >
          <UserHead
            user={user}
          />
        </div>
      </Option>
    ),
    avoidShowError: (props, List) => new Promise((resolve) => {
      const { value } = props;
      const extraList = [];
      const values = value instanceof Array ? value : [value];
      const requestQue = [];
      values.forEach((a) => {
        if (a && typeof a === 'string' && a !== '0' && !find(List, { id: a })) {
          requestQue.push(userApi.getById(a));
        }
      });
      Promise.all(requestQue).then((users) => {
        users.forEach((res) => {
          if (res.list && res.list.length > 0) {
            extraList.push(res.list[0]);
          }
        });
        resolve(extraList);
      }).catch(() => {
        resolve(extraList);
      });
    }),
  },
  issue_status: {
    request: () => statusApi.loadByProject('agile'),
    render: status => (
      <Option
        key={status.id}
        value={status.id}
        name={status.name}
      >
        {status.name}
      </Option>
    ),
  },
  status_program: {
    request: () => new Promise(resolve => statusApi.loadByProject('program').then((statusList) => {
      resolve(statusList);
    })),
    render: status => (
      <Option
        key={status.id}
        value={status.id}
        name={status.name}
      >
        <div style={{ display: 'inline-flex', alignItems: 'center', padding: '2px' }}>
          <StatusTag
            data={status}
          />
        </div>
      </Option>
    ),
  },
  epic: {
    props: {
      filterOption,
    },
    request: () => epicApi.loadEpicsForSelect(),
    render: epic => (
      <Option
        key={epic.issueId}
        value={epic.issueId}
      >
        {epic.epicName}
      </Option>
    ),
  },
  epic_program: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption:
        (input, option) => option.props.children
          && option.props.children.toLowerCase().indexOf(
            input.toLowerCase(),
          ) >= 0,
    },
    request: epicApi.loadProgramEpics,
    render: epic => (
      <Option
        key={epic.issueId}
        value={epic.issueId}
      >
        {epic.epicName}
      </Option>
    ),
  },
  issue_type_program,
  issue_type_program_simple: {
    ...issue_type_program,
    render: issueType => (
      <Option
        key={issueType.id}
        value={issueType.id}
        name={issueType.name}
      >
        {issueType.name}
      </Option>
    ),
  },
  issue_type: {
    request: () => issueTypeApi.loadAllWithStateMachineId('agile'),
    render: issueType => (
      <Option
        key={issueType.id}
        value={issueType.id}
        name={issueType.name}
      >
        {issueType.name}
      </Option>
    ),
  },
  issue_type_program_feature_epic: {
    ...issue_type_program,
    request: () => new Promise(resolve => issueTypeApi.loadAllWithStateMachineId('program').then((issueTypes) => {
      const featureTypes = [{
        id: 'business',
        name: '特性',
      }, {
        id: 'enabler',
        name: '使能',
      }];
      const epicType = find(issueTypes, { typeCode: 'issue_epic' });
      resolve([...featureTypes, epicType]);
    })),
    render: issueType => (
      <Option
        key={issueType.id}
        value={issueType.id}
        name={issueType.name}
      >
        {issueType.name}
      </Option>
    ),
  },
  issue_link: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: false,
      filterOption: false,
      loadWhenMount: true,
    },
    request: () => issueLinkTypeApi.getAll().then(res => transform(res.list)),
    render: link => (
      <Option value={`${link.linkTypeId}+${link.isIn}`}>
        {link.name}
      </Option>
    ),
  },
  issues_in_link: {
    props: {
      mode: 'multiple',
      optionLabelProp: 'showName',
      getPopupContainer: triggerNode => triggerNode.parentNode,
    },
    request: ({ filter, page }, issueId) => issueApi.loadIssuesInLink(page, 20, issueId, filter),
    render: issue => (
      <Option
        key={issue.issueId}
        value={issue.issueId}
        showName={issue.issueNum}
      >
        <div style={{
          display: 'inline-flex',
          flex: 1,
          width: 'calc(100% - 30px)',
          alignItems: 'center',
          verticalAlign: 'middle',
        }}
        >
          <TypeTag
            data={issue.issueTypeVO}
          />
          <span style={{
            paddingLeft: 12, paddingRight: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}
          >
            {issue.issueNum}
          </span>
          <div style={{ overflow: 'hidden', flex: 1 }}>
            <Tooltip title={issue.summary}>
              <p style={{
                paddingRight: '25px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: 0, maxWidth: 'unset',
              }}
              >
                {issue.summary}
              </p>
            </Tooltip>

          </div>
        </div>
      </Option>
    ),
  },
  features_in_link: {
    props: {
      mode: 'multiple',
      optionLabelProp: 'showName',
      getPopupContainer: triggerNode => triggerNode.parentNode,
    },
    request: ({ filter, page }, issueId) => featureApi.loadFeaturesInLink(page, 20, issueId, filter),
    render: issue => (
      <Option
        key={issue.featureId}
        value={issue.featureId}
        showName={issue.issueNum}
      >
        <div style={{
          display: 'inline-flex',
          flex: 1,
          width: 'calc(100% - 30px)',
          alignItems: 'center',
          verticalAlign: 'middle',
        }}
        >
          <TypeTag
            data={issue.issueTypeVO}
          />
          <span style={{
            paddingLeft: 12, paddingRight: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}
          >
            {issue.issueNum}
          </span>
          <div style={{ overflow: 'hidden', flex: 1 }}>
            <Tooltip title={issue.summary}>
              <p style={{
                paddingRight: '25px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: 0, maxWidth: 'unset',
              }}
              >
                {issue.summary}
              </p>
            </Tooltip>
          </div>
        </div>
      </Option>
    ),
  },
  priority: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: false,
      filterOption: false,
      loadWhenMount: true,
    },
    request: () => priorityApi.loadByProject(),
    getDefaultValue: priorities => find(priorities, { default: true }).id,
    render: priority => (
      <Option key={priority.id} value={priority.id}>
        <div style={{ display: 'inline-flex', alignItems: 'center', padding: 2 }}>
          <span>{priority.name}</span>
        </div>
      </Option>
    ),
  },
  component: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: false,
      filterOption: false,
      loadWhenMount: true,
    },
    request: ({ filter }) => componentApi.loadAllComponents(filter),
    render: component => (
      <Option
        key={component.name}
        value={component.name}
      >
        {component.name}
      </Option>
    ),
  },
  label: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: false,
      filterOption: false,
      loadWhenMount: true,
    },
    request: () => issueLabelApi.loads(),
    render: label => (
      <Option key={label.labelName} value={label.labelName}>
        {label.labelName}
      </Option>
    ),
  },
  label_id: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: true,
      filterOption,
      loadWhenMount: true,
    },
    request: () => issueLabelApi.loads(),
    render: label => (
      <Option key={label.labelId} value={label.labelId}>
        {label.labelName}
      </Option>
    ),
  },
  version: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filter: false,
      filterOption: false,
      loadWhenMount: true,
    },
    request: ({ filter, page }, statusList = ['version_planning']) => versionApi.loadNamesByStatus(statusList),
    render: version => (
      <Option
        key={version.versionId}
        value={version.versionId}
      >
        {version.name}
      </Option>
    ),
  },
  sprint: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      loadWhenMount: true,
    },
    request: ({ filter, page }, statusList = ['sprint_planning', 'started']) => sprintApi.loadSprints(statusList),
    render: sprint => (
      <Option key={sprint.sprintId} value={sprint.sprintId}>
        {sprint.sprintName}
      </Option>
    ),
  },
  sprint_in_project: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      loadWhenMount: true,
    },
    request: ({ filter, page }, { teamId, piId }) => sprintApi.loadSprintsByTeam(teamId, piId),
    render: sprint => (
      <Option key={sprint.sprintId} value={sprint.sprintId}>
        {sprint.sprintName}
      </Option>
    ),
  },
  pi: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      loadWhenMount: true,
    },
    request: () => piApi.getUnfinished(),
    render: pi => (
      <Option disabled={!IsInProgramStore.isOwner && pi.statusCode === 'doing'} key={pi.id} value={pi.id}>
        {`${pi.code}-${pi.name}`}
      </Option>
    ),
  },
  all_pi: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      onFilterChange: false,
      loadWhenMount: true,
      label: 'PI',
    },
    request: () => piApi.getPiListByStatus(),
    render: pi => (
      <Option disabled={!IsInProgramStore.isOwner && pi.statusCode === 'doing'} key={pi.id} value={pi.id}>
        {pi.code ? `${pi.code}-${pi.name}` : pi.name}
      </Option>
    ),
  },
  feature: {
    request: ({ filter, page }, requestArgs) => featureApi.getByEpicId(undefined, filter, page),
    render: item => (
      <Option key={`${item.issueId}`} value={item.issueId}>{item.summary}</Option>
    ),
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      loadWhenMount: true,
    },
  }, // 特性列表
  feature_all: {
    request: ({ filter, page }, requestArgs) => featureApi.queryAllInSubProject(requestArgs, filter, page),
    render: item => (
      <Option key={`${item.issueId}`} value={item.issueId}>{item.summary}</Option>
    ),
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      loadWhenMount: true,
    },
  }, // 特性列表
  sub_project: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption: filterOptionByName,
      onFilterChange: false,
      loadWhenMount: true,
    },
    request: () => commonApi.getSubProjects(true),
    render: pro => (
      <Option key={pro.projectId} value={pro.projectId} name={pro.projName}>
        <Tooltip title={pro.projName}>{pro.projName}</Tooltip>
      </Option>
    ),
  },
  sub_sprint: {
    props: {
      getPopupContainer: triggerNode => triggerNode.parentNode,
      filterOption,
      onFilterChange: false,
      loadWhenMount: true,
    },
    request: ({ filter, page }, { piId, teamIds }) => sprintApi.getTeamSprints(piId, teamIds),
    render: team => (
      <OptGroup label={team.projectVO.name} key={team.projectVO.id}>
        {(team.sprints || []).map(sprint => (
          <Option key={`${sprint.sprintId}`} value={sprint.sprintId}>
            <Tooltip placement="topRight" title={sprint.sprintName}>{sprint.sprintName}</Tooltip>
          </Option>
        ))}
      </OptGroup>
    ),
  },
};
