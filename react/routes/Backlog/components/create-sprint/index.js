import React, { useEffect, useMemo } from 'react';
import {
  Form, TextField, DataSet, TextArea, DateTimePicker, Icon,
} from 'choerodon-ui/pro';
import { sprintApi } from '@/api';
import { Choerodon } from '@choerodon/boot';
import { MAX_LENGTH_SPRINT } from '@/constants/MAX_LENGTH';
import BacklogStore from '@/stores/project/backlog/BacklogStore';

async function sprintNameValidator(value) {
  const isSame = await sprintApi.validate(value);
  return isSame ? '冲刺名称已存在' : true;
}
export default function CreateSprint({ modal: { handleOk, close }, onCreate }) {
  const dataSet = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [
      {
        name: 'sprintName', type: 'string', label: '冲刺名称', required: true, validator: sprintNameValidator,
      },
      {
        name: 'startDate',
        type: 'dateTime',
        label: '开始日期',
        dynamicProps: {
          max: ({ record }) => record.get('endDate'),
        },
      },
      {
        name: 'endDate',
        type: 'dateTime',
        label: '结束日期',
        dynamicProps: {
          min: ({ record }) => record.get('startDate'),
        },
      },
      {
        name: 'sprintGoal', type: 'string', label: '冲刺目标',
      },
    ],
  }), []);
  async function submit() {
    const isValidate = await dataSet.validate();
    if (isValidate) {
      const [values] = dataSet.toData();
      const sprint = await sprintApi.create(values);
      if (!sprint.failed) {
        onCreate(sprint);
        close();
      } else {
        Choerodon.prompt(sprint.message);
      }
    }
    return false;
  }
  useEffect(() => {
    handleOk(submit);
  }, [handleOk]);

  return (
    <Form dataSet={dataSet}>
      <TextField name="sprintName" required maxLength={MAX_LENGTH_SPRINT} />
      <DateTimePicker name="startDate" />
      <DateTimePicker name="endDate" />
      <TextArea
        rowSpan={2}
        colSpan={2}
        name="sprintGoal"
      />
    </Form>
  );
}
export function CreateCurrentPiSprint({
  modal: { handleOk, close }, onCreate, PiName, sprints, piId,
}) {
  function checkDateSame(value, name, record) {
    const startDate = record.get('startDate');
    const endDate = record.get('endDate');
    if (startDate && endDate && startDate.isSame(endDate)) {
      return `${name === 'endDate' ? '结束' : '开始'}时间与${name === 'endDate' ? '开始' : '结束'}时间相同`;
    }
    return true;
  }
  const dataSet = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [
      {
        name: 'sprintName', type: 'string', label: '冲刺名称', required: true, validator: sprintNameValidator,
      },
      {
        name: 'startDate',
        type: 'dateTime',
        label: '开始日期',
        required: true,
        validator: checkDateSame,
      },
      {
        name: 'endDate',
        type: 'dateTime',
        label: '结束日期',
        required: true,
        validator: checkDateSame,
      },
      {
        name: 'sprintGoal', type: 'string', label: '冲刺目标',
      },
    ],
  }), []);
  async function submit() {
    const isValidate = await dataSet.validate();
    if (isValidate) {
      const [values] = dataSet.toData();
      const sprint = await sprintApi.createOnCurrentPi({ ...values, piId });
      if (!sprint.failed) {
        onCreate(sprint);
        close();
      } else {
        Choerodon.prompt(sprint.message);
      }
    }
    return false;
  }

  useEffect(() => {
    handleOk(submit);
  }, [handleOk]);

  return (
    <Form dataSet={dataSet}>
      <div>
        <Icon type="info" style={{ marginBottom: '.04rem', marginRight: '.05rem', color: 'rgb(255,0,0,1)' }} />
        创建的冲刺将自动关联当前PI：
        {PiName}
      </div>
      <TextField name="sprintName" required maxLength={MAX_LENGTH_SPRINT} />
      <DateTimePicker
        name="startDate"
        filter={(date) => {   
          // 没选结束时间的时候，只判断时间点能不能选
          if (!dataSet.current.get('endDate')) {
            return BacklogStore.dateCanChoose({ date });
          } else {
            // 选了结束时间之后，判断形成的时间段是否和其他重叠
            return BacklogStore.rangeCanChoose({ startDate: date, endDate: dataSet.current.get('endDate') });
          }
        }}
      />
      <DateTimePicker
        name="endDate"
        filter={(date) => {
          // 没选开始时间的时候，只判断时间点能不能选
          if (!dataSet.current.get('startDate')) {
            return BacklogStore.dateCanChoose({ date });
          } else {
            // 选了开始时间之后，判断形成的时间段是否和其他重叠
            return BacklogStore.rangeCanChoose({ startDate: dataSet.current.get('startDate'), endDate: date });
          }          
        }}
      />
      <TextArea
        rowSpan={2}
        colSpan={2}
        name="sprintGoal"
      />
    </Form>
  );
}
