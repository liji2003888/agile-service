import React, {
  useEffect, useState, useCallback,
} from 'react';
import { observer } from 'mobx-react-lite';
import {
  Icon, Row, Col,
} from 'choerodon-ui';
import {
  Issue, IField, User, IIssueType,
} from '@/common/types';
import {
  includes, map, uniq, compact, flatten, find,
} from 'lodash';
import { unstable_batchedUpdates as batchedUpdates } from 'react-dom';
import DataSetField from 'choerodon-ui/pro/lib/data-set/Field';
import TypeTag from '@/components/TypeTag';
import { DataSet, Tooltip } from 'choerodon-ui/pro';
import {
  fieldApi, moveIssueApi, issueApi, userApi,
} from '@/api';
import styles from './Confirm.less';
import transformValue, { IFieldWithValue } from './transformValue';
import renderField from './renderField';
import store from '../../store';

export interface IssueWithSubIssueVOList extends Omit<Issue, 'subIssueVOList'> {
  subIssueVOList: Issue[]
}

export interface ILoseItems {
  test: boolean,
  doc: boolean,
  backlog: boolean,
  linkIssue: boolean,
}
interface Props {
  issue: any,
  dataSet: DataSet,
  fieldsWithValue: IFieldWithValue[]
  targetProjectType: 'program' | 'project' | 'subProject'
  targetIssueType?: IIssueType
  targetSubTaskType?: IIssueType
  targetSubBugType?: IIssueType
  loseItems: ILoseItems,
}

const Confirm: React.FC<Props> = ({
  issue, dataSet, fieldsWithValue, targetProjectType, targetIssueType, targetSubTaskType, targetSubBugType, loseItems,
}) => {
  const {
    dataMap, selfFields, subTaskFields, subBugFields, moveToProjectList, subTaskDetailMap, subBugDetailMap, subTaskTypeId, subBugTypeId, selectedUserIds, selectedUsers,
  } = store;
  const [fieldsLosed, setFieldsLosed] = useState<IField[]>([]);
  const {
    issueId, issueNum, summary, typeCode, subIssueVOList, subBugVOList, epicName,
  } = issue;
  const targetProjectId = dataSet?.current?.get('targetProjectId');
  const issueType = dataSet?.current?.get('issueType');

  const addField = useCallback((name, props) => {
    const field = new DataSetField({ ...props, name }, dataSet, dataSet.current);
    dataSet?.current?.fields.set(name, field);
  }, [dataSet]);

  const filterFields = useCallback((arr: IField[]) => {
    const excludeCodes: string[] = ['summary', 'issueType', 'description', 'remainingTime', 'storyPoints', 'priority', 'estimatedStartTime', 'estimatedEndTime', 'benfitHypothesis', 'acceptanceCritera', 'environment'];
    if (typeCode === 'feature') {
      excludeCodes.push('sprint');
    }
    return arr.filter((item) => (item.system || (!item.system && !item.projectId && item.fieldType === 'member')) && !includes(excludeCodes, item.fieldCode));
  }, [typeCode]);

  const getFinalFields = useCallback((res) => {
    const statusField = {
      fieldName: '状态',
      fieldCode: 'status',
      system: true,
      code: 'status',
      required: true,
    } as IField;

    const reporterField = {
      fieldName: '报告人',
      fieldCode: 'reporter',
      system: true,
      code: 'reporter',
      required: true,
    } as IField;
    const resAdded = [
      ...(res || []),
      reporterField,
    ];
    // 后端返回了就不加了
    if (!find(res, { fieldCode: 'status' })) {
      resAdded.unshift(statusField);
    }
    const filtered = filterFields(resAdded);
    if (targetProjectType === 'subProject' && targetIssueType?.typeCode === 'story') {
      const epicFieldIndex = filtered.findIndex((item) => item.fieldCode === 'epic');
      if (epicFieldIndex > -1) {
        const featureField = {
          fieldName: '特性',
          fieldCode: 'feature',
          system: true,
          code: 'feature',
        } as IField;
        filtered.splice(epicFieldIndex, 1, featureField);
      }
    }
    return filtered;
  }, [filterFields, targetIssueType?.typeCode, targetProjectType]);

  useEffect(() => {
    if (targetProjectId && issueType) {
      fieldApi.getFields({
        issueTypeId: issueType,
        pageCode: 'agile_issue_create',
        schemeCode: 'agile_issue',
      }, targetProjectId).then((res: IField[]) => {
        batchedUpdates(() => {
          const finalFields = getFinalFields(res || []);
          finalFields.forEach((item) => {
            if (item.fieldCode !== 'epicName') {
              addField(`${issueId}-${item.fieldCode}`, {
                required: item.required,
              });
            } else {
              addField(`${issueId}-${item.fieldCode}`, {
                required: item.required,
                maxLength: 20,
              });
            }
          });
          store.setSelfFields(finalFields);
        });
      });
      if (subIssueVOList && subIssueVOList.length) {
        fieldApi.getFields({
          issueTypeId: subTaskTypeId as string,
          pageCode: 'agile_issue_create',
          schemeCode: 'agile_issue',
        }, targetProjectId).then((res: IField[]) => {
          batchedUpdates(() => {
            const finalFields = getFinalFields(res || []);
            subIssueVOList.forEach((subTask: Issue) => {
              finalFields.forEach((item) => {
                addField(`${subTask.issueId}-${item.fieldCode}`, {
                  required: item.required,
                });
              });
            });
            store.setSubTaskFields(finalFields);
          });
        });
      }
      if (subBugVOList && subBugVOList.length) {
        fieldApi.getFields({
          issueTypeId: subBugTypeId as string,
          pageCode: 'agile_issue_create',
          schemeCode: 'agile_issue',
        }, targetProjectId).then((res: IField[]) => {
          batchedUpdates(() => {
            const finalFields = getFinalFields(res || []);
            subBugVOList.forEach((subBug: Issue) => {
              finalFields.forEach((item) => {
                addField(`${subBug.issueId}-${item.fieldCode}`, {
                  required: item.required,
                });
              });
            });
            store.setSubBugFields(finalFields);
          });
        });
      }
    }
  }, [addField, getFinalFields, issueId, issueType, subBugTypeId, subBugVOList, subIssueVOList, subTaskTypeId, targetProjectId]);

  useEffect(() => {
    if (targetProjectId && issueId && targetIssueType?.id) {
      moveIssueApi.getFieldsLosed(targetProjectId, issueId, targetIssueType?.id).then((res: IField[]) => {
        setFieldsLosed(res);
      });
    }
  }, [issueId, targetIssueType?.id, targetProjectId]);

  useEffect(() => {
    if (subTaskTypeId) {
      const detailRequestArr: Promise<Issue>[] = [];
      const customFieldsRequestArr: Promise<IFieldWithValue>[] = [];
      subIssueVOList.forEach((subTask: any) => {
        detailRequestArr.push(issueApi.load(subTask.issueId));
      });
      let subTaskSelectedUserIds: any[] = [...store.selectedUserIds];
      Promise.all(detailRequestArr).then((res: Issue[]) => {
        batchedUpdates(() => {
          res.forEach((subTask) => {
            subTaskSelectedUserIds = [...subTaskSelectedUserIds, ...[subTask.assigneeId, subTask.reporterId, subTask.mainResponsible?.id]];
            subTaskDetailMap.set(`${subTask.issueId}%detail`, subTask);
          });
          const uniqUserIds = uniq(compact(subTaskSelectedUserIds));
          store.setSelectUserIds(uniqUserIds);
        });
        return res;
      }).then((issueDetails) => {
        subIssueVOList.forEach((subTask: any, index: number) => {
          customFieldsRequestArr.push(fieldApi.getFieldAndValue(subTask.issueId, {
            schemeCode: 'agile_issue',
            issueTypeId: issueDetails[index].issueTypeId as string,
            pageCode: 'agile_issue_edit',
          }));
        });
        Promise.all(customFieldsRequestArr).then((res: IFieldWithValue[]) => {
          res.forEach((fieldWidthValue, i) => {
            batchedUpdates(() => {
              fieldsWithValue.forEach((item) => {
                if (!item.system && item.fieldType === 'member' && !item.projectId) {
                  subTaskSelectedUserIds = [...subTaskSelectedUserIds, item.value];
                }
              });
              const uniqUserIds = uniq(compact(subTaskSelectedUserIds));
              store.setSelectUserIds(uniqUserIds);
              subTaskDetailMap.set(`${subIssueVOList[i].issueId}%fields`, fieldWidthValue);
            });
          });
        });
      });
    }
  }, [fieldsWithValue, subIssueVOList, subTaskDetailMap, subTaskTypeId]);

  useEffect(() => {
    if (subBugTypeId) {
      const detailRequestArr: Promise<Issue>[] = [];
      const customFieldsRequestArr: Promise<IFieldWithValue>[] = [];
      subBugVOList.forEach((subBug: any) => {
        detailRequestArr.push(issueApi.load(subBug.issueId));
      });
      let subBugSelectedUserIds: any[] = [...store.selectedUserIds];
      Promise.all(detailRequestArr).then((res: Issue[]) => {
        batchedUpdates(() => {
          res.forEach((subBug) => {
            subBugSelectedUserIds = [...subBugSelectedUserIds, ...[subBug.assigneeId, subBug.reporterId, subBug.mainResponsible?.id]];
            subBugDetailMap.set(`${subBug.issueId}%detail`, subBug);
          });
          const uniqUserIds = uniq(compact(subBugSelectedUserIds));
          store.setSelectUserIds(uniqUserIds);
        });
        return res;
      }).then((issueDetails) => {
        subIssueVOList.forEach((subBug: any, index: number) => {
          customFieldsRequestArr.push(fieldApi.getFieldAndValue(subBug.issueId, {
            schemeCode: 'agile_issue',
            issueTypeId: issueDetails[index].issueTypeId as string,
            pageCode: 'agile_issue_edit',
          }));
        });
        Promise.all(customFieldsRequestArr).then((res: IFieldWithValue[]) => {
          res.forEach((fieldWidthValue, i) => {
            batchedUpdates(() => {
              fieldsWithValue.forEach((item) => {
                if (!item.system && item.fieldType === 'member' && !item.projectId) {
                  subBugSelectedUserIds = [...subBugSelectedUserIds, item.value];
                }
              });
              const uniqUserIds = uniq(compact(subBugSelectedUserIds));
              store.setSelectUserIds(uniqUserIds);
              subBugDetailMap.set(`${subBugVOList[i].issueId}%fields`, fieldWidthValue);
            });
          });
        });
      });
    }
  }, [fieldsWithValue, subBugDetailMap, subBugTypeId, subBugVOList, subIssueVOList]);

  const targetProject = moveToProjectList.find((item: any) => item.id === targetProjectId) || { name: '' };

  useEffect(() => {
    const selectedUserRequestArr: Promise<any[]>[] = [];
    if (selectedUserIds && selectedUserIds.length) {
      selectedUserIds.forEach((id) => {
        selectedUserRequestArr.push(userApi.project(targetProjectId).getById(id));
      });
      Promise.all(selectedUserRequestArr).then((res) => {
        store.setSelectedUsers(flatten(flatten(res).map((item) => item.list)));
      });
    }
  }, [selectedUserIds, targetProjectId]);

  useEffect(() => {
    const memberFieldsCodeAndValue = new Map([
      [`${issue.issueId}-assignee`, issue.assigneeId],
      [`${issue.issueId}-reporter`, issue.reporterId],
      [`${issue.issueId}-mainResponsible`, issue.mainResponsible?.id],
    ]);
    fieldsWithValue.forEach((field) => {
      const {
        fieldCode, projectId, fieldType, system, value,
      } = field;
      if (!system && !projectId && fieldType === 'member' && value) {
        memberFieldsCodeAndValue.set(`${issue.issueId}-${fieldCode}`, value);
      }
    });

    for (const [k, v] of subTaskDetailMap.entries()) {
      const subTaskIssueId = k.split('%')[0];
      const isDetail = k.split('%')[1] === 'detail';
      if (isDetail) {
        memberFieldsCodeAndValue.set(`${subTaskIssueId}-assignee`, v.assigneeId);
        memberFieldsCodeAndValue.set(`${subTaskIssueId}-reporter`, v.reporterId);
        memberFieldsCodeAndValue.set(`${subTaskIssueId}-mainResponsible`, v.mainResponsible?.id);
      } else {
        v.forEach((field: IFieldWithValue) => {
          const {
            fieldCode, projectId, fieldType, system, value,
          } = field;
          if (!system && !projectId && fieldType === 'member' && value) {
            memberFieldsCodeAndValue.set(`${subTaskIssueId}-${fieldCode}`, value);
          }
        });
      }
    }
    for (const [k, v] of subBugDetailMap.entries()) {
      const subBugIssueId = k.split('%')[0];
      const isDetail = k.split('%')[1] === 'detail';
      if (isDetail) {
        memberFieldsCodeAndValue.set(`${subBugIssueId}-assignee`, v.assigneeId);
        memberFieldsCodeAndValue.set(`${subBugIssueId}-reporter`, v.reporterId);
        memberFieldsCodeAndValue.set(`${subBugIssueId}-mainResponsible`, v.mainResponsible?.id);
      } else {
        v.forEach((field: IFieldWithValue) => {
          const {
            fieldCode, projectId, fieldType, system, value,
          } = field;
          if (!system && !projectId && fieldType === 'member' && value) {
            memberFieldsCodeAndValue.set(`${subBugIssueId}-${fieldCode}`, value);
          }
        });
      }
    }
    batchedUpdates(() => {
      for (const [k, v] of memberFieldsCodeAndValue.entries()) {
        if (v && selectedUsers.find((user) => user.id === v)) {
          dataSet.current?.set(k, v);
        } else {
          dataSet.current?.set(k, undefined);
        }
      }
    });

    return () => {
      memberFieldsCodeAndValue.clear();
    };
  }, [dataSet, fieldsWithValue, issue.assigneeId, issue.issueId, issue.mainResponsible?.id, issue.reporterId, selectedUsers, subBugDetailMap, subTaskDetailMap]);

  useEffect(() => {
    if (epicName && !dataSet.current?.get(`${issueId}-epicName`)) {
      dataSet.current?.set(`${issueId}-epicName`, epicName);
    }
  }, [dataSet, epicName, issueId]);

  const loseItemNames = [];

  for (const [k, v] of Object.entries(loseItems)) {
    const loseItemMap = new Map([
      ['test', '测试用例'],
      ['doc', '文档'],
      ['backlog', '需求'],
      ['linkIssue', '问题项'],
    ]);
    if (v) {
      loseItemNames.push(loseItemMap.get(k));
    }
  }

  let tipText = `${issue.issueNum}将移动到【${targetProject?.name}】中`;

  if (fieldsLosed.length || loseItemNames.length) {
    if (fieldsLosed.length) {
      tipText += `，${fieldsLosed.map((item) => `【${item.name}】`).join('、')}的字段值将永久丢失。`;
    }

    if (loseItemNames.length) {
      if (fieldsLosed.length) {
        tipText += `且该问题与其他${loseItemNames.join('、')}的关联关系，将永久丢失。`;
      } else {
        tipText += `，该问题与其他${loseItemNames.join('、')}的关联关系，将永久丢失。`;
      }
    }
  } else {
    tipText += '。';
  }

  return (
    <div className={styles.confirm}>
      <div className={styles.tip}>
        <Icon type="report" />
        <p className={styles.tipText}>
          {tipText}
        </p>
      </div>
      <div className={styles.content}>
        <div className={styles.contentTip}>
          系统将保留兼容的字段值，您可以根据需要更新以下不兼容的字段值：
        </div>
        <div className={styles.contentMain}>
          <div className={styles.issueItem}>
            <div className={styles.issueItemHeader}>
              <TypeTag data={targetIssueType as IIssueType} />
              <span className={styles.issueNum}>{issueNum}</span>
              <span className={styles.summary}>{summary}</span>
            </div>
            <div className={styles.issueItemFields}>
              <Row key={`${issue.issueId}-fieldHeader`} className={styles.fieldHeaderRow}>
                <Col span={7}>
                  <span className={styles.fieldHeader}>字段</span>
                </Col>
                <Col span={8}>
                  <span className={styles.fieldHeader}>原始值</span>
                </Col>
                <Col span={9}>
                  <span className={styles.fieldHeader}>更新值</span>
                </Col>
              </Row>
              {
                selfFields.map((selfField) => {
                  const { fieldCode, fieldName } = selfField;
                  const transformedOriginValue = transformValue({ issue, field: selfField, fieldsWithValue });
                  return (
                    <Row key={fieldCode} className={styles.fieldRow}>
                      <Col span={7}>
                        <span className={`${styles.fieldReadOnly} ${styles.fieldNameCol}`}>
                          {fieldName}
                          {
                            dataSet.current?.getField(`${issueId}-${fieldCode}`)?.props?.required && (
                              <span className={styles.required}>*</span>
                            )
                          }
                        </span>
                      </Col>
                      <Col span={8}>
                        <Tooltip title={transformedOriginValue}>
                          <span className={styles.fieldReadOnly}>{transformedOriginValue}</span>
                        </Tooltip>
                      </Col>
                      <Col span={9}>
                        {renderField({
                          dataSet,
                          issue,
                          field: selfField,
                          fieldsWithValue,
                          targetIssueType,
                          targetProject: {
                            projectId: targetProjectId,
                            projectType: targetProjectType,
                          },
                          dataMap,
                          selectedUsers,
                          isSelf: true,
                        })}
                      </Col>
                    </Row>
                  );
                })
              }
            </div>
          </div>
          {
            subTaskTypeId ? subIssueVOList.map((subTask: any) => (
              <div className={styles.issueItem}>
                <div className={styles.issueItemHeader}>
                  {targetSubTaskType && <TypeTag data={targetSubTaskType} />}
                  <span className={styles.issueNum}>{subTask.issueNum}</span>
                  <span className={styles.summary}>{subTask.summary}</span>
                </div>
                <div className={styles.issueItemFields}>
                  <Row key={`${subTask.issueId}-fieldHeader`} className={styles.fieldHeaderRow}>
                    <Col span={7}>
                      <span className={styles.fieldHeader}>字段</span>
                    </Col>
                    <Col span={8}>
                      <span className={styles.fieldHeader}>原始值</span>
                    </Col>
                    <Col span={9}>
                      <span className={styles.fieldHeader}>更新值</span>
                    </Col>
                  </Row>
                  {
                    subTaskFields.map((subTaskField) => {
                      const { fieldCode, fieldName } = subTaskField;
                      const subTaskDetail = subTaskDetailMap.get(`${subTask.issueId}%detail`) || {};
                      const subTaskCustomFields = subTaskDetailMap.get(`${subTask.issueId}%fields`) || [];
                      const transformedOriginValue = transformValue({ issue: subTaskDetail, field: subTaskField, fieldsWithValue: subTaskCustomFields });
                      return (
                        <Row key={fieldCode} className={styles.fieldRow}>
                          <Col span={7}>
                            <span className={`${styles.fieldReadOnly} ${styles.fieldNameCol}`}>
                              {fieldName}
                              {
                                dataSet.current?.getField(`${subTask.issueId}-${fieldCode}`)?.props?.required && (
                                  <span className={styles.required}>*</span>
                                )
                              }
                            </span>
                          </Col>
                          <Col span={8}>
                            <Tooltip title={transformedOriginValue}>
                              <span className={styles.fieldReadOnly}>{transformedOriginValue}</span>
                            </Tooltip>
                          </Col>
                          <Col span={9}>
                            {renderField({
                              dataSet,
                              issue: subTaskDetail,
                              field: subTaskField,
                              fieldsWithValue: subTaskCustomFields,
                              targetIssueType: {
                                typeCode: 'sub_task',
                                id: subTaskTypeId,
                              } as IIssueType,
                              targetProject: {
                                projectId: targetProjectId,
                                projectType: targetProjectType,
                              },
                              dataMap,
                              selectedUsers,
                            })}
                          </Col>
                        </Row>
                      );
                    })
                  }
                </div>
              </div>
            )) : null
          }
          {
            subBugTypeId ? subBugVOList.map((subBug: any) => (
              <div className={styles.issueItem}>
                <div className={styles.issueItemHeader}>
                  {targetSubBugType && <TypeTag data={targetSubBugType} />}
                  <span className={styles.issueNum}>{subBug.issueNum}</span>
                  <span className={styles.summary}>{subBug.summary}</span>
                </div>
                <div className={styles.issueItemFields}>
                  <Row key={`${subBug.issueId}-fieldHeader`} className={styles.fieldHeaderRow}>
                    <Col span={7}>
                      <span className={styles.fieldHeader}>字段</span>
                    </Col>
                    <Col span={8}>
                      <span className={styles.fieldHeader}>原始值</span>
                    </Col>
                    <Col span={9}>
                      <span className={styles.fieldHeader}>更新值</span>
                    </Col>
                  </Row>
                  {
                    subBugFields.map((subBugField) => {
                      const { fieldCode, fieldName } = subBugField;
                      const subBugDetail = subBugDetailMap.get(`${subBug.issueId}%detail`) || {};
                      const subBugCustomFields = subBugDetailMap.get(`${subBug.issueId}%fields`) || [];
                      const transformedOriginValue = transformValue({ issue: subBugDetail, field: subBugField, fieldsWithValue: subBugCustomFields });
                      return (
                        <Row key={fieldCode} className={styles.fieldRow}>
                          <Col span={7}>
                            <span className={`${styles.fieldReadOnly} ${styles.fieldNameCol}`}>
                              {fieldName}
                              {
                                dataSet.current?.getField(`${subBug.issueId}-${fieldCode}`)?.props?.required && (
                                  <span className={styles.required}>*</span>
                                )
                              }
                            </span>
                          </Col>
                          <Col span={8}>
                            <Tooltip title={transformedOriginValue}>
                              <span className={styles.fieldReadOnly}>{transformedOriginValue}</span>
                            </Tooltip>
                          </Col>
                          <Col span={9}>
                            {renderField({
                              dataSet,
                              issue: subBugDetail,
                              field: subBugField,
                              fieldsWithValue: subBugCustomFields,
                              targetIssueType: {
                                typeCode: 'bug',
                                id: subBugTypeId,
                              } as IIssueType,
                              targetProject: {
                                projectId: targetProjectId,
                                projectType: targetProjectType,
                              },
                              dataMap,
                              selectedUsers,
                            })}
                          </Col>
                        </Row>
                      );
                    })
                  }
                </div>
              </div>
            )) : null
          }
        </div>
      </div>
    </div>
  );
};

export default observer(Confirm);
