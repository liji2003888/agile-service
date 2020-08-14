import React, { Component } from 'react';
import { Permission } from '@choerodon/boot';
import './Column.less';

import StatusList from './StatusList';


class Column extends Component {
  render() {
    const { data } = this.props;
    return (
      <div
        className="c7n-scrumsetting-column"
        style={{
          flex: 1,
          height: '100%',
        }}
      >
        <div
          className="c7n-scrumsetting-columnContent"
          style={{
            background: 'white',
          }}
        >
          <div className="c7n-scrumsetting-columnTop">
            <div className="c7n-scrumsetting-columnStatus">
              {data.name}
            </div>
            <div style={{ borderBottom: '3px solid rgba(0,0,0,0.26)' }} className="c7n-scrumsetting-columnBottom">             
              <div>
                <span>无该问题的状态</span>
              </div>               
            </div>
          </div>
          <Permission
            service={['choerodon.code.project.cooperation.iteration-plan.ps.movetocolomn']}
            noAccessChildren={(
              <StatusList data={data} isDragDisabled />
                )}
          >
            <StatusList data={data} />
          </Permission>
        </div>
      </div>
    );
  }
}

export default Column;
