import React, { useRef } from 'react';
import { CheckBox } from 'choerodon-ui/pro';
import { find } from 'lodash';
import SelectSprint from '@/components/select/select-sprint';
import { LabelLayout } from 'choerodon-ui/pro/lib/form/enum';
import { ISprint } from '@/common/types';

export interface SprintSearchProps {
  sprintId: string | undefined
  setSprintId: (sprintId: string | undefined) => void
  currentSprintId: string | undefined
  setCurrentSprintId: (sprintId: string | undefined) => void
  setEndDate: (endDate: string) => void
  restDayShow: boolean
  setRestDayShow: (restDayShow: boolean) => void
  useCurrentSprint?: boolean
  setUseCurrentSprint: (useCurrentSprint: boolean) => void
  projectId?:string
  onEmpty: () => void
}
const SprintSearch: React.FC<SprintSearchProps> = ({
  sprintId,
  setSprintId,
  currentSprintId,
  setCurrentSprintId,
  setEndDate,
  restDayShow,
  setRestDayShow,
  useCurrentSprint,
  setUseCurrentSprint,
  projectId,
  onEmpty,
}) => {
  const sprintsRef = useRef<ISprint[]>([]);
  return (
    <div>
      <SelectSprint
        label="迭代冲刺"
        labelLayout={'float' as LabelLayout}
        style={{ width: 500 }}
        clearButton={false}
        projectId={projectId}
        statusList={['started', 'closed']}
        currentSprintOption
      // onOption={({ record }) => ({
      //   disabled: record.get('sprintId') === '0' && !currentSprintId,
      // })}
        afterLoad={(sprints) => {
          sprintsRef.current = sprints;
          const current = find(sprints, { statusCode: 'started' });
          if (current) {
            setCurrentSprintId(current.sprintId);
          }
          if (useCurrentSprint && !currentSprintId) {
            if (current) {
              setSprintId(current.sprintId);
              setEndDate(current.endDate);
            } else {
              setSprintId(undefined);
              setEndDate('');
              onEmpty();
            }
          } else if (sprints.length > 0) {
            if (!sprintId) {
              setSprintId(sprints[0].sprintId);
              setEndDate(sprints[0].endDate);
            } else {
              const target = find(sprints, { sprintId });
              if (target) {
                setEndDate(target.endDate);
              }
            }
          }
        }}
        value={useCurrentSprint ? 'current' : sprintId}
        primitiveValue={false}
        onChange={(sprint: ISprint | null) => {
          if (sprint && sprint.sprintId === 'current') {
            setSprintId(currentSprintId);
            const target = find(sprintsRef.current, { sprintId });
            if (target) {
              setEndDate(target.endDate);
            }
            setUseCurrentSprint(true);
            return;
          }
          if (sprint) {
            setSprintId(sprint.sprintId);
            setEndDate(sprint.endDate);
            setUseCurrentSprint(false);
          } else {
            setSprintId(undefined);
            setEndDate('');
            setUseCurrentSprint(false);
          }
        }}
      />
      <CheckBox
        style={{ marginLeft: 24 }}
        checked={restDayShow}
        onChange={setRestDayShow}
      >
        显示非工作日
      </CheckBox>
    </div>
  );
};
export default SprintSearch;
