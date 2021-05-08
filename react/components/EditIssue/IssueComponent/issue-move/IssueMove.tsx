import React, {
  useMemo, useState, useEffect, useCallback, useRef,
} from 'react';
import { observer } from 'mobx-react-lite';
import {
  DataSet, Modal, Button,
} from 'choerodon-ui/pro';
import { Steps } from 'choerodon-ui';
import {
  includes, map, compact, uniq,
} from 'lodash';
import { toJS } from 'mobx';
import {
  IModalProps, AppStateProps, IIssueType, IField,
} from '@/common/types';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import { ButtonColor, FuncType } from 'choerodon-ui/pro/lib/button/enum';
import { stores, Choerodon } from '@choerodon/boot';
import {
  issueTypeApi, projectApi, moveIssueApi, commonApi,
} from '@/api';
import Field from 'choerodon-ui/pro/lib/data-set/Field';
import SelectProject from './components/select-project';
import Confirm from './components/confirm-data';
import styles from './IssueMove.less';
import { IssueWithSubIssueVOList, ILoseItems } from './components/confirm-data/Confirm';
import transformValue, { submitFieldMap } from './transformValue';
import { IFieldWithValue } from './components/confirm-data/transformValue';

import store from './store';
import { split } from './utils';

const isDEV = process.env.NODE_ENV === 'development';
// @ts-ignore
const HAS_AGILE_PRO = C7NHasModule('@choerodon/agile-pro');
const shouldRequest = isDEV || HAS_AGILE_PRO;
const { AppState }: { AppState: AppStateProps } = stores;
const { Step } = Steps;

interface Props {
  issue: IssueWithSubIssueVOList,
  modal?: IModalProps
  fieldsWithValue: IFieldWithValue[]
  onMoveIssue: () => void,
  loseItems: ILoseItems,
}

const IssueMove: React.FC<Props> = ({
  modal, issue, fieldsWithValue, onMoveIssue, loseItems,
}) => {
  const { dataMap } = store;
  const [, setUpdateCount] = useState<number>(0);
  const [currentStep, setCurrentStep] = useState<number>(1);
  const [submitBtnDisable, setSubmitBtnDisable] = useState<boolean>(true);
  const [btnLoading, setBtnLoading] = useState<boolean>(false);
  const [targetProjectType, setTargetProjectType] = useState<'program' | 'project' | 'subProject'>('project');
  const issueTypeDataSet = useMemo(() => new DataSet({
    paging: false,
    data: [],
  }), []);

  const removeField = useCallback((ds: DataSet, name: string) => {
    ds?.fields?.delete(name);
    ds?.current?.fields.delete(name);
  }, []);

  const resetData = useCallback((ds: DataSet, excludeFieldsCode: string[]) => {
    const fieldNames: string[] = [];
    (ds.current?.fields || []).forEach((field: Field) => {
      if (!includes(excludeFieldsCode, field.get('name'))) {
        field.reset();
        ds.current?.set(field.get('name'), undefined);
        fieldNames.push(field.get('name'));
      }
    });
    fieldNames.forEach((name: string) => {
      removeField(ds, name);
    });
    store.dataMap.clear();
  }, [removeField]);

  const dataSet = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [
      {
        name: 'targetProjectId',
        textField: 'name',
        valueField: 'id',
        label: '选择目标项目',
        required: true,
      },
      {
        name: 'issueType',
        textField: 'name',
        valueField: 'id',
        label: '选择目标类型',
        options: issueTypeDataSet,
        required: true,
        dynamicProps: {
          disabled: ({ record }) => issue.typeCode === 'feature' || !record.get('targetProjectId'),
        },
      }, {
        name: 'subTaskIssueTypeId',
        textField: 'name',
        valueField: 'id',
        label: '选择子任务类型',
        options: issueTypeDataSet,
        dynamicProps: {
          disabled: ({ record }) => !record.get('targetProjectId'),
          required: () => issue.subIssueVOList?.length > 0,
        },
      }, {
        name: 'subBugIssueTypeId',
        textField: 'name',
        valueField: 'id',
        label: '选择子缺陷类型',
        options: issueTypeDataSet,
        dynamicProps: {
          disabled: ({ record }) => !record.get('targetProjectId'),
          required: () => issue.subBugVOList?.length > 0,
        },
      },
    ],
    events: {
      update: async ({
        // @ts-ignore
        dataSet: moveDataSet, name, value,
      }) => {
        if (name === 'targetProjectId') {
          if (value) {
            const targetProjectInfo = await projectApi.loadBasicInfo(value);
            let targetIsInProgram = false;
            const targetIsProgram = (targetProjectInfo.categories || []).find((item: any) => item.code === 'N_PROGRAM');
            if (!targetIsInProgram && shouldRequest) {
              targetIsInProgram = Boolean(await commonApi.getProjectsInProgram(value));
            }
            const issueTypes: IIssueType[] = await issueTypeApi.loadAllWithStateMachineId(targetIsProgram ? 'program' : 'agile', value);
            const excludeTypeCode: string[] = [];
            if (!targetIsProgram) {
              excludeTypeCode.push('feature');
              if (targetIsInProgram) {
                setTargetProjectType('subProject');
                excludeTypeCode.push('issue_epic');
              } else {
                setTargetProjectType('project');
              }
              // if (issue.subIssueVOList && issue.subIssueVOList.length) {
              //   excludeTypeCode.push('bug');
              // }
            } else {
              setTargetProjectType('program');
            }
            await issueTypeDataSet.loadData(issueTypes.filter((item) => !includes(excludeTypeCode, item.typeCode)));
          } else {
            issueTypeDataSet.loadData([]);
          }
          if (issue.typeCode === 'feature') {
            moveDataSet.current.set('issueType', 'feature');
          } else if (moveDataSet.current?.get('issueType')) {
            moveDataSet.current.set('issueType', undefined);
          }
        }
        if (name === 'targetProjectId' || name === 'issueType' || name === 'subTaskIssueTypeId' || name === 'subBugIssueTypeId') {
          resetData(moveDataSet, ['targetProjectId', 'issueType', 'subTaskIssueTypeId', 'subBugIssueTypeId']); // 改变项目或者问题类型应该重置
        }
        if (name === `${issue.issueId}-sprint` && issue.subIssueVOList && issue.subIssueVOList.length) {
          issue.subIssueVOList.forEach((subTask) => {
            moveDataSet.current?.set(`${subTask.issueId}-sprint`, value);
          });
        }
        if (name === `${issue.issueId}-sprint` && issue.subBugVOList && issue.subBugVOList.length) {
          issue.subBugVOList.forEach((subTask) => {
            moveDataSet.current?.set(`${subTask.issueId}-sprint`, value);
          });
        }
        if (name !== 'targetProjectId' && name !== 'issueType' && name !== 'subTaskIssueTypeId' && name !== 'subBugIssueTypeId') {
          const validate = await moveDataSet.validate();
          setSubmitBtnDisable(!validate);
        }
        if (name === 'subTaskIssueTypeId') {
          store.setSubTaskTypeId(value);
        }
        if (name === 'subBugIssueTypeId') {
          store.setSubBugTypeId(value);
        }
        setUpdateCount((count) => count + 1);
      },
    },
  }), [issue.issueId, issue.subBugVOList, issue.subIssueVOList, issue.typeCode, issueTypeDataSet, resetData]);

  const handlePre = () => {
    setCurrentStep(currentStep - 1);
  };
  const handleNext = async () => {
    Promise.all([dataSet.current?.getField('targetProjectId')?.checkValidity(), dataSet.current?.getField('issueType')?.checkValidity(), dataSet.current?.getField('subTaskIssueTypeId')?.checkValidity(), dataSet.current?.getField('subBugIssueTypeId')?.checkValidity()]).then((validateRes) => {
      if (validateRes.every((validate) => !!validate)) {
        setCurrentStep(currentStep + 1);
      }
    });
  };
  const handleCancel = () => {
    dataSet.reset();
    modal?.close();
  };

  useEffect(() => {
    const userIds = [issue.assigneeId, issue.reporterId, issue.mainResponsible?.id, ...(map(fieldsWithValue.filter((item) => !item.system && item.fieldType === 'member' && !item.projectId), 'value'))];
    const uniqUserIds = uniq(compact(userIds));
    store.setSelectUserIds(uniqUserIds);
  }, [fieldsWithValue, issue.assigneeId, issue.mainResponsible?.id, issue.reporterId]);

  const { selfFields, subTaskFields, subBugFields } = store;
  const targetTypeId = dataSet.current?.get('issueType');
  const subTaskIssueTypeId = dataSet.current?.get('subTaskIssueTypeId');
  const subBugIssueTypeId = dataSet.current?.get('subBugIssueTypeId');
  const targetProjectId = dataSet.current?.get('targetProjectId');

  const handleSubmit = useCallback(() => {
    setBtnLoading(true);
    let submitData: any = {
      issueId: issue.issueId,
      issueTypeId: targetTypeId,
      subIssues: [],
    };
    for (const [k, v] of Object.entries(dataSet.current?.data || {})) {
      const kIssueId = split(k, '-')[0];
      const isSelf = kIssueId === issue.issueId;
      if (kIssueId && kIssueId !== 'undefined' && split(k, '-')[1] && v) {
        const isSubTask = issue.subIssueVOList?.find((item) => item.issueId === kIssueId);
        let fields = selfFields;
        if (!isSelf) {
          if (isSubTask) {
            fields = subTaskFields;
          } else {
            fields = subBugFields;
          }
        }
        const fieldInfo = fields.find((item: IField) => item.fieldCode === split(k, '-')[1]);
        if (fieldInfo) {
          if (fieldInfo.system) { // 系统字段
            if (submitFieldMap.get(split(k, '-')[1])) {
              const fieldAndValue = {
                [submitFieldMap.get(split(k, '-')[1]) as string]: transformValue({
                  k,
                  v,
                  dataMap,
                  targetProjectId,
                }),
              };
              if (isSelf) {
                submitData = {
                  ...submitData,
                  ...fieldAndValue,
                };
                if (k.indexOf('fixVersion') > -1) {
                  submitData = {
                    ...submitData,
                    versionType: 'fix',
                  };
                }
              } else {
                const currentSubIssueItemIndex = submitData.subIssues.findIndex((item: any) => item.issueId === kIssueId);
                if (currentSubIssueItemIndex === -1) {
                  submitData.subIssues.push({
                    issueId: kIssueId,
                    issueTypeId: isSubTask ? subTaskIssueTypeId : subBugIssueTypeId,
                    ...fieldAndValue,
                  });
                } else {
                  submitData.subIssues[currentSubIssueItemIndex] = {
                    ...submitData.subIssues[currentSubIssueItemIndex],
                    ...fieldAndValue,
                  };
                }
                if (k.indexOf('fixVersion') > -1) {
                  submitData.subIssues[currentSubIssueItemIndex] = {
                    ...submitData.subIssues[currentSubIssueItemIndex],
                    versionType: 'fix',
                  };
                }
              }
            }
          } else if (isSelf) { // 非系统字段，自己
            if (submitData.customFields) {
              submitData.customFields.push({
                fieldId: fieldInfo.fieldId,
                fieldType: fieldInfo.fieldType,
                value: v,
              });
            } else {
              submitData.customFields = [{
                fieldId: fieldInfo.fieldId,
                fieldType: fieldInfo.fieldType,
                value: v,
              }];
            }
          } else { // 非系统字段，子任务 | 子缺陷
            const currentSubIssueItemIndex = submitData.subIssues.findIndex((item: any) => item.issueId === kIssueId);
            if (currentSubIssueItemIndex === -1) {
              submitData.subIssues.push({
                issueId: kIssueId,
                issueTypeId: isSubTask ? subTaskIssueTypeId : subBugIssueTypeId,
                customFields: [
                  {
                    fieldId: fieldInfo.fieldId,
                    fieldType: fieldInfo.fieldType,
                    value: v,
                  },
                ],
              });
            } else if (submitData.subIssues[currentSubIssueItemIndex].customFields) {
              submitData.subIssues[currentSubIssueItemIndex].customFields.push({
                fieldId: fieldInfo.fieldId,
                fieldType: fieldInfo.fieldType,
                value: v,
              });
            } else {
              submitData.subIssues[currentSubIssueItemIndex].customFields = [
                {
                  fieldId: fieldInfo.fieldId,
                  fieldType: fieldInfo.fieldType,
                  value: v,
                },
              ];
            }
          }
        }
      }
    }
    moveIssueApi.moveIssueToProject(issue.issueId, targetProjectId, submitData).then(() => {
      dataSet.reset();
      onMoveIssue();
      Choerodon.prompt('移动成功');
      setBtnLoading(false);
      modal?.close();
    }).catch(() => {
      setBtnLoading(false);
      Choerodon.prompt('移动失败');
    });
    return false;
  }, [dataMap, dataSet, issue.issueId, issue.subIssueVOList, modal, onMoveIssue, selfFields, subBugFields, subBugIssueTypeId, subTaskFields, subTaskIssueTypeId, targetProjectId, targetTypeId]);

  const targetIssueType = issueTypeDataSet.toData().find((item: IIssueType) => item.id === targetTypeId) as IIssueType;
  const targetSubTaskType = issueTypeDataSet.toData().find((item: IIssueType) => item.id === subTaskIssueTypeId) as IIssueType;
  const targetSubBugType = issueTypeDataSet.toData().find((item: IIssueType) => item.id === subBugIssueTypeId) as IIssueType;
  return (
    <div className={styles.issueMove}>
      <Steps current={currentStep - 1}>
        <Step
          title={<span style={{ color: currentStep === 1 ? '#3F51B5' : '', fontSize: 14 }}>选择项目和问题类型</span>}
        />
        <Step
          title={<span style={{ color: currentStep === 2 ? '#3F51B5' : '', fontSize: 14 }}>确认数据信息</span>}
        />
      </Steps>
      <div className={styles.step_content}>
        {currentStep === 1 && <SelectProject issue={issue} dataSet={dataSet} issueTypeDataSet={issueTypeDataSet} />}
        {currentStep === 2 && (
          <Confirm
            issue={issue}
            dataSet={dataSet}
            fieldsWithValue={fieldsWithValue}
            targetProjectType={targetProjectType}
            targetIssueType={targetIssueType}
            targetSubTaskType={targetSubTaskType}
            targetSubBugType={targetSubBugType}
            loseItems={loseItems}
          />
        )}
      </div>
      <div className={styles.steps_action}>
        {currentStep === 1 && (
          <>
            <Button color={'primary' as ButtonColor} funcType={'raised' as FuncType} onClick={handleNext}>
              下一步
            </Button>
            <Button onClick={handleCancel} funcType={'raised' as FuncType}>
              取消
            </Button>
          </>
        )}
        {currentStep === 2 && (
          <>
            <Button style={{ marginLeft: 8 }} color={'primary' as ButtonColor} funcType={'raised' as FuncType} onClick={handlePre}>
              上一步
            </Button>
            <Button color={'primary' as ButtonColor} funcType={'raised' as FuncType} onClick={handleSubmit} disabled={submitBtnDisable} loading={btnLoading}>
              确认
            </Button>
            <Button onClick={handleCancel} funcType={'raised' as FuncType}>
              取消
            </Button>
          </>
        )}
      </div>
    </div>
  );
};

const ObserverIssueMove = observer(IssueMove);

const openIssueMove = ({
  issue, customFields, onMoveIssue, loseItems,
}: { issue: IssueWithSubIssueVOList, customFields: IFieldWithValue[], onMoveIssue: () => void, loseItems: ILoseItems }) => {
  Modal.open({
    key: 'issueMoveModal',
    drawer: true,
    title: '移动问题',
    className: styles.issueMoveModal,
    style: {
      width: MODAL_WIDTH.middle,
    },
    children: <ObserverIssueMove issue={issue} fieldsWithValue={customFields} onMoveIssue={onMoveIssue} loseItems={loseItems} />,
    footer: null,
  });
};

export default openIssueMove;
