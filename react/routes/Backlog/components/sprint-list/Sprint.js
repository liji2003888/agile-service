import React, { Fragment } from 'react';
import { observer } from 'mobx-react-lite';
import BacklogStore from '@/stores/project/backlog/BacklogStore';
import IssueList from './IssueList';
import SprintHeader from './SprintHeader';

function Sprint({ data }) {
  const { sprintId, expand } = data;
  const issueList = BacklogStore.getIssueListBySprintId(sprintId);
  const dataSet = BacklogStore.getDataSet(sprintId);
  return (
    <div style={{ width: '100%' }}>
      <SprintHeader data={data} />
      {expand && (
        <>
          {dataSet && <IssueList sprintData={data} dataSet={dataSet} data={issueList} sprintId={sprintId} />}
        </>
      )}
    </div>
  );
}

export default observer(Sprint);
