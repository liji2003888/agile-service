import React from 'react';
import { withRouter } from 'react-router-dom';
import { Tooltip } from 'choerodon-ui';
import { issueLink } from '@/utils/link';


const IssueNumber = ({
  parentIssueId, resetIssue, reloadIssue, typeCode, parentSummary, issue, type, history, disabled,
}) => {
  const handleClickParent = () => {
    if (disabled) {
      return false;
    }
    if (reloadIssue) {
      reloadIssue(parentIssueId);
    }
    if (resetIssue) {
      resetIssue(parentIssueId);
    }
    return false;
  };

  const handleClickIssueNum = () => {
    if (disabled) {
      return false;
    }
    const { issueId, issueNum } = issue;
    history.push(issueLink(issueId, typeCode, issueNum));
    return false;
  };


  const { issueNum } = issue;
  return (
    <div style={{
      fontSize: 16, lineHeight: '28px', fontWeight: 500, whiteSpace: 'nowrap',
    }}
    >
      {
        parentSummary ? (
          <span style={{ display: 'inline-block', width: '90%', maxWidth: 'max-content' }}>
            <Tooltip title={parentSummary}>
              <span
                role="none"
                className="primary parent-summary-hidden"
                style={{ cursor: disabled ? 'auto' : 'pointer' }}
                onClick={handleClickParent}
              >
                {parentSummary}
              </span>
            </Tooltip>

            <span style={{ paddingLeft: 10, paddingRight: 10 }}>/</span>
          </span>
        ) : null
      }
      {
        ((['sub_task', 'bug'].includes(typeCode) && parentSummary) || typeCode === 'feature') ? (
          <span>
            {issueNum}
          </span>
        ) : (
          <a
            role="none"
            onClick={handleClickIssueNum}
            style={{ cursor: disabled ? 'auto' : 'pointer' }}
          >
            {issueNum}
          </a>
        )
      }
    </div>
  );
};

export default withRouter(IssueNumber);
