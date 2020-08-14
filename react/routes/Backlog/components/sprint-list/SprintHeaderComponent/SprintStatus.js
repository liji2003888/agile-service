import React from 'react';
import { observer } from 'mobx-react';
import './SprintStatus.less';

const prefix = 'c7n-backlog-SprintStatus';

function SprintStatus({
  data: { statusCode, planning },
}) {
  return statusCode === 'started' ? (         
    <div className={`${prefix} ${prefix}-active`}>
      活跃
    </div>
  ) : (         
    <div className={prefix}>
      {planning === true ? '规划中' : '未开始'}
    </div>
  );
}

export default observer(SprintStatus);
