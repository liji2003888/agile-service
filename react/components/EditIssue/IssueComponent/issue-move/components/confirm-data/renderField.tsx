import React from 'react';
import { map, includes } from 'lodash';
import STATUS from '@/constants/STATUS';
import { Tooltip } from 'choerodon-ui';
import { stores } from '@choerodon/boot';
import { toJS } from 'mobx';
import {
  IField, IStatus, User, ILabel, IIssueType,
} from '@/common/types';
import TextEditToggle from '@/components/TextEditTogglePro';
import SelectStatus from '@/components/select/select-status';
import SelectComponent from '@/components/select/select-component';
import SelectLabel from '@/components/select/select-label';
import SelectEpic from '@/components/select/select-epic';
import SelectFixVersion from '@/components/select/select-version';
import SelectSprint from '@/components/select/select-sprint';
import SelectUser from '@/components/select/select-user';
import SelectPI from '@/components/select/select-pi';
import SelectTeam from '@/components/select/select-team';
import SelectProgramVersion from '@/components/select/select-program-version';
import SelectFeature from '@/components/select/select-feature';
import { DataSet, TextField } from 'choerodon-ui/pro/lib';
import { epicApi } from '@/api';
import UserTag from '@/components/tag/user-tag';
import store from '../../store';

const { AppState } = stores;
export interface IFieldWithValue extends IField {
  value: any,
  valueStr: any,
}

interface Props {
  dataSet: DataSet,
  issue: any,
  field: IField,
  fieldsWithValue: IFieldWithValue[],
  targetIssueType?: IIssueType,
  targetProject: {
    projectId: string,
    projectType: 'program' | 'project' | 'subProject',
  },
  dataMap: Map<string, any>,
  disabled?: boolean,
  selectedUsers: User[]
  isSelf?: boolean
}

const submit = () => {};

const transToArr = (arr: any, pro: string, type = 'string') => {
  if (!arr.length) {
    return type === 'string' ? '无' : [];
  } if (typeof arr[0] === 'object') {
    return type === 'string' ? map(arr, pro).join() : map(arr, pro);
  }
  return type === 'string' ? arr.join() : arr;
};

const renderComponent = (name: string, symbol = ',') => {
  if (name && [...name].length > 20) {
    return (
      <Tooltip title={name}>
        <span>
          {name.substring(0, 20)}
          ...
        </span>
        {symbol}
      </Tooltip>
    );
  }
  return (
    <span>
      {name}
      {symbol}
    </span>
  );
};

const getFieldValue = (dataSet: DataSet, name: string) => dataSet.current?.get(name);

const renderField = ({
  dataSet, issue, field, fieldsWithValue, targetIssueType, targetProject, dataMap, disabled = false, selectedUsers, isSelf = false,
}: Props) => {
  const {
    fieldCode, system, fieldType, projectId,
  } = field;
  const {
    issueId,
  } = issue;
  switch (fieldCode) {
    case 'status': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-status`);
      const fieldValueItem = dataMap.get('status')?.find((item: any) => item.id === fieldValue);
      return (
        <TextEditToggle
          key={`${issueId}-status`}
          disabled={disabled}
          className="moveIssue-textEditToggle"
          initValue={undefined}
          onSubmit={submit}
          editor={() => (
            <SelectStatus
              key={`${issueId}-status`}
              dataSet={dataSet}
              name={`${issueId}-status`}
              issueTypeId={targetIssueType?.id}
              projectId={targetProject.projectId}
              applyType={targetProject.projectType === 'program' ? 'program' : 'agile'}
              afterLoad={(data) => {
                dataMap.set('status', data);
                const defaultStatus = (data || []).find((item: any) => item.defaultStatus);
                const statusValue = dataSet.current?.get(`${issueId}-status`);
                if (!statusValue) {
                  dataSet.current?.set(`${issueId}-status`, defaultStatus?.id);
                }
              }}
              clearButton={false}
            />
          )}
        >
          {
            fieldValueItem ? (
              <div
                style={{
                  background: STATUS[fieldValueItem.type as IStatus['valueCode']],
                  color: '#fff',
                  borderRadius: '2px',
                  padding: '0 8px',
                  display: 'inline-block',
                  margin: '2px auto 2px 0',
                }}
              >
                {fieldValueItem.name}
              </div>
            ) : '无'
        }
        </TextEditToggle>
      );
    }
    case 'component': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-component`) || [];
      const fieldValueItem = dataMap.get('component')?.filter((item: any) => includes(fieldValue, item.componentId)) || [];
      const newComponents = fieldValue.filter((item: string) => !includes(map(fieldValueItem, 'componentId'), item));
      const allFieldValueItem = [...fieldValueItem, ...(newComponents.map((name: string) => ({ name })))];
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectComponent
              dataSet={dataSet}
              name={`${issueId}-component`}
              valueField="name"
              projectId={targetProject.projectId}
              afterLoad={(data) => {
                dataMap.set('component', data);
              }}
            />
          )}
        >
          {allFieldValueItem && allFieldValueItem.length
            ? (
              <div>
                <p className="primary" style={{ wordBreak: 'break-word', marginTop: 2 }}>
                  {transToArr(allFieldValueItem, 'name', 'array').map((item: any, index: number, arr: any[]) => renderComponent(item, index === arr.length - 1 ? '' : undefined))}
                </p>
              </div>
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'label': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-label`) || [];
      const fieldValueItem = dataMap.get('label')?.filter((item: any) => includes(fieldValue, item.labelId)) || [];
      const newLabels = (fieldValue || []).filter((item: string) => !includes(map(fieldValueItem, 'labelId'), item));
      const allFieldValueItem = [...fieldValueItem, ...(newLabels.map((name: string) => ({ labelName: name })))];
      const issueLabelNames = (issue.labelIssueRelVOList || []).map((item: ILabel) => ({
        labelName: item.labelName, projectId: targetProject.projectId,
      }));
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          editor={() => (
            <SelectLabel
              combo
              dataSet={dataSet}
              name={`${issueId}-label`}
              valueField="labelName"
              projectId={targetProject.projectId}
              afterLoad={(data) => {
                dataMap.set('label', data);
                dataSet.current?.set(`${issueId}-label`, issueLabelNames.map((item: { labelName: string}) => item.labelName));
              }}
              extraOptions={issueLabelNames}
            />
          )}
        >
          {allFieldValueItem && allFieldValueItem.length
            ? (
              <div>
                <p className="primary" style={{ wordBreak: 'break-word', marginTop: 2 }}>
                  {transToArr(allFieldValueItem, 'labelName', 'array').map((item: any, index: number, arr: any[]) => renderComponent(item, index === arr.length - 1 ? '' : undefined))}
                </p>
              </div>
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'epic': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-epic`);
      const fieldValueItem = dataMap.get('epic')?.find((item: any) => item.issueId === fieldValue);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectEpic
              dataSet={dataSet}
              name={`${issueId}-epic`}
              afterLoad={(data) => {
                dataMap.set('epic', data);
              }}
              dontAddEpic0
              request={() => (targetProject.projectType === 'program' ? epicApi.project(targetProject.projectId).loadProgramEpics() : epicApi.loadEpicsForSelect(targetProject.projectId))}
            />
          )}
        >
          {
            fieldValueItem ? (
              <div
                style={{
                  color: fieldValueItem.epicColor,
                  borderWidth: '1px',
                  borderStyle: 'solid',
                  borderColor: fieldValueItem.epicColor,
                  borderRadius: '2px',
                  fontSize: '13px',
                  lineHeight: '20px',
                  padding: '0 8px',
                  display: 'inline-block',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {fieldValueItem.epicName}
              </div>
            ) : '无'
        }
        </TextEditToggle>
      );
    }
    case 'fixVersion': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-fixVersion`) || [];
      const fieldValueItem = dataMap.get('fixVersion')?.filter((item: any) => includes(fieldValue, item.versionId)) || [];
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectFixVersion
              dataSet={dataSet}
              name={`${issueId}-fixVersion`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('fixVersion', data);
              }}
              statusArr={['version_planning']}
              valueField="versionId"
            />
          )}
        >
          {fieldValueItem && fieldValueItem.length
            ? (
              <p className="primary" style={{ wordBreak: 'break-word' }}>
                {map(fieldValueItem, 'name').join(' , ')}
              </p>
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'sprint': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-sprint`);
      const fieldValueItem = dataMap.get('sprint')?.find((item: any) => fieldValue === item.sprintId);
      return (
        <TextEditToggle
          disabled={disabled || !isSelf}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectSprint
              dataSet={dataSet}
              name={`${issueId}-sprint`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('sprint', data);
              }}
              statusList={['sprint_planning', 'started']}
              isProgram={targetProject.projectType === 'program'}
            />
          )}
        >
          {fieldValueItem
            ? (
              <p
                style={{
                  color: isSelf ? '#4d90fe' : 'var(--text-color3)',
                  fontSize: '13px',
                  lineHeight: '20px',
                  display: 'inline-block',
                }}
              >
                {fieldValueItem.sprintName}
              </p>
            ) : (
              <div style={{
                color: isSelf ? '#000000' : 'var(--text-color3)',
              }}
              >
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'assignee': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-assignee`);
      const fieldValueItem = [...selectedUsers, ...(dataMap.get('assignee') || [])].find((item: any) => fieldValue === item.id);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectUser
              dataSet={dataSet}
              key={`${issueId}-assignee`}
              name={`${issueId}-assignee`}
              projectId={targetProject.projectId}
              autoQueryConfig={{
                selectedUserIds: issue.assigneeId,
              }}
              afterLoad={(data: any) => {
                console.log('data：');
                console.log(data);
                dataMap.set('assignee', data);
              }}
            />
          )}
        >
          {fieldValueItem
            ? (
              <UserTag
                data={fieldValueItem}
              />
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'pi': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-pi`);
      const fieldValueItem = dataMap.get('pi')?.find((item: any) => fieldValue === item.id);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectPI
              dataSet={dataSet}
              name={`${issueId}-pi`}
              projectId={targetProject.projectId}
              statusList={['doing, todo']}
              afterLoad={(data: any) => {
                dataMap.set('pi', data);
              }}
            />
          )}
        >
          {fieldValueItem ? (
            <div
              style={{
                color: '#4d90fe',
                fontSize: '13px',
                lineHeight: '20px',
                display: 'inline-block',
              }}
            >
              {`${fieldValueItem.code}-${fieldValueItem.name}`}
            </div>
          ) : (
            <div>
              无
            </div>
          )}
        </TextEditToggle>
      );
    }
    case 'subProject': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-subProject`) || [];
      const fieldValueItem = dataMap.get('subProject')?.filter((item: any) => includes(fieldValue, item.projectId)) || [];
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectTeam
              dataSet={dataSet}
              name={`${issueId}-subProject`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('subProject', data);
              }}
              multiple
            />
          )}
        >
          {fieldValueItem && fieldValueItem.length
            ? (
              fieldValueItem.map((team: any) => team.projName).join(' 、 ')
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'programVersion': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-programVersion`) || [];
      const fieldValueItem = dataMap.get('programVersion')?.filter((item: any) => includes(fieldValue, item.id)) || [];
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectProgramVersion
              dataSet={dataSet}
              name={`${issueId}-programVersion`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('programVersion', data);
              }}
              multiple
            />
          )}
        >
          {fieldValueItem && fieldValueItem.length
            ? (
              <p className="primary" style={{ wordBreak: 'break-word' }}>
                {fieldValueItem && fieldValueItem.length > 0
                  ? fieldValueItem.map((item: any, index: number, arr: any[]) => renderComponent(item.name, index === arr.length - 1 ? '' : undefined)) : '无'}
              </p>
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'feature': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-feature`);
      const fieldValueItem = dataMap.get('feature')?.find((item: any) => fieldValue === item.issueId);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectFeature
              dataSet={dataSet}
              name={`${issueId}-feature`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('feature', data);
                if (issue.featureId && data.find((item: any) => item.issueId === issue.featureId)) {
                  dataSet.current?.set(`${issueId}-feature`, issue.featureId);
                }
              }}
            />
          )}
        >
          {fieldValueItem ? (
            <div
              className="primary"
              style={{ wordBreak: 'break-word' }}
            >
              {fieldValueItem.summary}
            </div>
          ) : (
            <div>
              无
            </div>
          )}
        </TextEditToggle>
      );
    }
    case 'mainResponsible': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-mainResponsible`);
      const fieldValueItem = [...selectedUsers, ...(dataMap.get('mainResponsible') || [])].find((item: any) => fieldValue === item.id);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectUser
              dataSet={dataSet}
              name={`${issueId}-mainResponsible`}
              projectId={targetProject.projectId}
              autoQueryConfig={{
                selectedUserIds: issue.mainResponsible?.id,
              }}
              afterLoad={(data: any) => {
                dataMap.set('mainResponsible', data);
              }}
            />
          )}
        >
          {fieldValueItem
            ? (
              <UserTag
                data={fieldValueItem}
              />
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'reporter': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-reporter`);
      const fieldValueItem = [...selectedUsers, ...(dataMap.get('reporter') || []), AppState.userInfo].find((item: any) => fieldValue === item.id);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectUser
              dataSet={dataSet}
              name={`${issueId}-reporter`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set('reporter', data);
              }}
              autoQueryConfig={{
                selectedUserIds: fieldValue,
              }}
            />
          )}
        >
          {fieldValueItem
            ? (
              <UserTag
                data={fieldValueItem}
              />
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
    case 'epicName': {
      const fieldValue = getFieldValue(dataSet, `${issueId}-epicName`);
      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          editor={() => (
            <TextField
              dataSet={dataSet}
              name={`${issueId}-epicName`}
            />
          )}
        >
          {fieldValue || '无'}
        </TextEditToggle>
      );
    }
    default:
      break;
  }
  if (!system && fieldType === 'member' && !projectId) {
    const fieldItem = fieldsWithValue.find((item: IFieldWithValue) => item.fieldCode === fieldCode);
    if (fieldItem && fieldItem.fieldCode) {
      const fieldValue = getFieldValue(dataSet, `${issueId}-${fieldItem.fieldCode}`);
      const fieldValueItem = [...selectedUsers, ...(dataMap.get(fieldItem.fieldCode) || [])].find((item: any) => fieldValue === item.id);

      return (
        <TextEditToggle
          disabled={disabled}
          className="moveIssue-textEditToggle"
          onSubmit={submit}
          initValue={undefined}
          alwaysRender={false}
          editor={() => (
            <SelectUser
              dataSet={dataSet}
              name={`${issueId}-${fieldCode}`}
              projectId={targetProject.projectId}
              afterLoad={(data: any) => {
                dataMap.set(fieldItem.fieldCode as string, data);
              }}
              autoQueryConfig={{
                selectedUserIds: issue.value,
              }}
            />
          )}
        >
          {fieldValueItem
            ? (
              <UserTag
                data={fieldValueItem}
              />
            ) : (
              <div>
                无
              </div>
            )}
        </TextEditToggle>
      );
    }
  }
  return '';
};

export default renderField;
