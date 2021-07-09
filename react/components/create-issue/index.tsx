import React, {
  useEffect, useRef, useState, useMemo, useReducer,
} from 'react';
import {
  DataSet, Modal, Row, Col, Spin, Form,
} from 'choerodon-ui/pro';
import { observer } from 'mobx-react-lite';
import { usePersistFn } from 'ahooks';
import { merge, isEmpty } from 'lodash';
import { FieldType } from 'choerodon-ui/pro/lib/data-set/enum';
// @ts-ignore
import UploadButton from '@/components/CommonComponent/UploadButton';
import validateFile from '@/utils/File';
import useProjectIssueTypes from '@/hooks/data/useProjectIssueTypes';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import { IModalProps, IssueCreateFields } from '@/common/types';
import useIssueCreateFields from '@/hooks/data/useIssueCreateFields';
import valueTypeMap from './fields';

interface CreateIssueProps {
  // onCreate: (demand: Demand) => void,
  modal: IModalProps,
  // project?: IAMProject,
}
const defaultDataSet = new DataSet({
  autoCreate: true,
  fields: [],
});
const presets = new Map([
  ['estimatedStartTime', {
    max: 'estimatedEndTime',
  }],
  ['estimatedEndTime', {
    min: 'estimatedStartTime',
  }],
]);
const lineField = ['summary', 'description'];
const reuseFields = ['issueType', 'summary', 'description'];
function convert(fields: IssueCreateFields[]) {
  return fields.map((field) => ({
    ...field,
    title: field.fieldName,
    code: field.fieldCode,
    colSpan: lineField.includes(field.fieldCode) ? 2 : 1,
  }));
}
function transformSubmitFieldValue(field: IssueCreateFields, value: any) {
  switch (field.fieldType) {
    case 'time':
    case 'date':
    case 'datetime': {
      return value ? value.format('YYYY-MM-DD HH:mm:ss') : value;
    }
    default: return value;
  }
}
const CreateIssue = observer(({
  modal,
}: CreateIssueProps) => {
  const [fileList, setFileList] = useState<FileList>();
  const dataSetRef = useRef(defaultDataSet);
  const [dataSet, setDataSet] = useState(defaultDataSet);
  dataSetRef.current = dataSet;
  const setFieldValue = usePersistFn((name, value) => {
    dataSet.current?.set(name, value);
  });
  const { isLoading } = useProjectIssueTypes({

  }, {
    onSuccess: ((issueTypes) => {
      setFieldValue('issueType', issueTypes[0].id);
    }),
  });
  const issueTypeId = dataSet.current && dataSet.current.get('issueType');
  const handleUpdate = usePersistFn(({ name, value }) => {
    switch (name) {
      case 'issueType': {
        break;
      }
      default: break;
    }
  });
  const { data: fields } = useIssueCreateFields({ issueTypeId });
  useEffect(() => {
    const oldDataSet = dataSetRef.current;
    const newDataSet = new DataSet({
      autoCreate: true,
      fields: fields ? fields.map((field) => {
        if (presets.has(field.fieldCode)) {
          const preset = presets.get(field.fieldCode);
          return merge(preset, {
            label: field.fieldName,
            required: field.required,
          });
        }
        return {
          name: field.fieldCode,
          type: 'string' as FieldType,
          label: field.fieldName,
          required: field.required,
        };
      }) : [],
    });
    reuseFields.forEach((name) => {
      const oldValue = oldDataSet.current?.get(name);
      if (oldValue) {
        newDataSet?.current?.set(name, oldValue);
      }
    });
    setDataSet(newDataSet);
  }, [fields]);
  const handleSubmit = usePersistFn(async () => {
    if (await dataSet.validate()) {
      const data = dataSet.current?.toData();
      const customFields = fields?.filter((f) => !f.system);
      const fieldList = customFields?.map((field) => ({
        fieldType: field.fieldType,
        value: transformSubmitFieldValue(field, data[field.fieldCode]),
        fieldId: field.fieldId,
        fieldCode: field.fieldCode,
      }));
      const {
        typeId,
        reporterId,
        summary,
        description,
        // storyPoints,
        // estimatedTime,
        // sprintId,
        // statusId,
        // epicId,
        // pi,
        // epicName,
        // assigneedId,
        // benfitHypothesis,
        // acceptanceCritera,
        // featureType,
        // componentIssueRel,
        // priorityId,
        // issueLabel,
        // fixVersionIssueRel,
        // influenceVersion,
        // linkTypes,
        // linkIssues,
        // keys,
        // fileList,
        // userBusinessValue,
        // timeCriticality,
        // rrOeValue,
        // jobSize,
        // featureId,
        // teamProjectIds,
        // estimatedEndTime,
        // estimatedStartTime,
        // subBugParent,
        // subTaskParent,
        // programVersion,
        // environment,
        // appVersions,
        // tags,
        // subProjectSprintId,
        // mainResponsibleId,
        // testResponsibleId,
      } = data;
      console.log(data, fieldList);
    }
    return false;
  });
  useEffect(() => {
    modal.handleOk(handleSubmit);
  }, [handleSubmit, modal]);
  const renderFields = usePersistFn(() => (
    <Row gutter={24}>
      {fields?.map((field) => {
        const config = valueTypeMap[field.fieldCode] ?? valueTypeMap[field.fieldType] ?? valueTypeMap.default;
        return config ? (
          <Col
            key={field.fieldCode}
            style={{ marginBottom: 24 }}
            span={lineField.includes(field.fieldCode) ? 24 : 12}
          >
            {config.renderFormItem({
              name: field.fieldCode,
              style: {
                width: '100%',
              },
            })}
          </Col>
        ) : null;
      })}
    </Row>
  ));
  return (
    <Spin spinning={isLoading}>
      <Form
        dataSet={dataSet}
      >
        {renderFields()}
        <UploadButton
          fileList={fileList}
          colSpan={2}
          // @ts-ignore
          onChange={({ fileList: files }) => {
            if (validateFile(files)) {
              setFileList(files);
            }
          }}
        />
      </Form>
    </Spin>
  );
});

const openModal = () => {
  Modal.open({
    drawer: true,
    style: {
      width: MODAL_WIDTH.middle,
    },
    key: 'create-issue',
    title: '创建问题',
    okText: '创建',
    // @ts-ignore
    children: <CreateIssue />,
  });
};
export default openModal;
