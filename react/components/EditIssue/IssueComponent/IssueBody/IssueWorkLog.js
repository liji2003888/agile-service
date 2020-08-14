import React, { useContext } from 'react';
import { observer } from 'mobx-react-lite';
import { Icon, Button, Tooltip } from 'choerodon-ui';
import { FormattedMessage } from 'react-intl';
import Log from '../../Component/Log';
import EditIssueContext from '../../stores';
import Divider from './Divider';

const IssueWorkLog = observer(({
  hasPermission,
  reloadIssue,
}) => {
  const { store, disabled } = useContext(EditIssueContext);

  const renderLogs = () => {
    const worklogs = store.getWorkLogs || [];
    return (
      <div className="c7n-log-list">
        {
          worklogs.map(worklog => (
            <Log
              key={worklog.logId}
              worklog={worklog}
              onDeleteLog={reloadIssue}
              onUpdateLog={reloadIssue}
              hasPermission={hasPermission}
            />
          ))
        }
      </div>
    );
  };

  return (
    <div id="log">
      <div className="c7n-title-wrapper">
        <div className="c7n-title-left">
          <FormattedMessage id="issue.log" />
        </div>
        {!disabled && (
          <div className="c7n-title-right" style={{ marginLeft: '14px' }}>
            <Tooltip placement="topRight" title="登记工作" getPopupContainer={triggerNode => triggerNode.parentNode}>
              <Button style={{ padding: '0 6px' }} className="leftBtn" funcType="flat" onClick={() => store.setWorkLogShow(true)}>
                <Icon type="playlist_add icon" />
              </Button>
            </Tooltip>
          </div>
        )}
      </div>
      {renderLogs()}
      <Divider />
    </div>
  );
});

export default IssueWorkLog;
