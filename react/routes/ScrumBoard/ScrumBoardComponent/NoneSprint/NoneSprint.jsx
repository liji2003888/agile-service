/* eslint-disable react/self-closing-comp,jsx-a11y/accessible-emoji */
import React from 'react';
import { observer } from 'mobx-react-lite';
import { EmptyPage } from '@choerodon/components';
import EmptyScrumboard from './emptyScrumboard.svg';

const NoneSprint = ({ doingSprintExist, filterItems, hasSetFilter }) => {
  let tipTitle = '没有活跃的Sprint';
  const { sprint } = filterItems;
  if ((doingSprintExist || sprint) && Object.keys(filterItems).length === 1) {
    tipTitle = '当前冲刺下未规划问题';
  } else if (hasSetFilter) {
    tipTitle = '当前筛选条件下无问题';
  }

  return (
    <>
      <EmptyPage
        image={EmptyScrumboard}
        description={(
          <>
            {tipTitle}
            ，
            在工作列表的
            <EmptyPage.Button style={{ color: '#5365EA' }}>待办事项</EmptyPage.Button>
            {!doingSprintExist ? '中开启冲刺' : '规划问题到当前冲刺'}
          </>
        )}
      >
      </EmptyPage>
    </>
  );
};

export default observer(NoneSprint);
