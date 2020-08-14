import React, { Component } from 'react';
import { observer, inject } from 'mobx-react';
import { withRouter } from 'react-router-dom';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import { issueApi } from '@/api';
import SelectLabel from '@/components/select/select-label';
import TextEditToggle from '@/components/TextEditTogglePro';


@inject('AppState')
@observer class FieldLabel extends Component {
  dataRef = React.createRef();

  transToArr = (arr, pro, type = 'string') => {
    if (!arr.length) {
      return type === 'string' ? '无' : [];
    } else if (typeof arr[0] === 'object') {
      return type === 'string' ? _.map(arr, pro).join() : _.map(arr, pro);
    } else {
      return type === 'string' ? arr.join() : arr;
    }
  };

  updateIssueLabel = (labels) => {
    const newLabelIssueRelVOList = labels || [];
    const originLabels = this.dataRef.current;
    const {
      store, onUpdate, reloadIssue, AppState,
    } = this.props;
    const issue = store.getIssue;
    const { labelIssueRelVOList = [], issueId, objectVersionNumber } = issue;
    if (JSON.stringify(labelIssueRelVOList) !== JSON.stringify(newLabelIssueRelVOList)) {
      const labelList = [];
      newLabelIssueRelVOList.forEach((label) => {
        const target = _.find(originLabels, { labelName: label });
        if (target) {
          labelList.push(target);
        } else {
          labelList.push({
            labelName: label,
            projectId: AppState.currentMenuType.id,
          });
        }
      });
      const obj = {
        issueId,
        objectVersionNumber,
        labelIssueRelVOList: labelList,
      };
      issueApi.update(obj)
        .then(() => {
          if (onUpdate) {
            onUpdate();
          }
          if (reloadIssue) {
            reloadIssue(issueId);
          }
        });
    }
  };

  render() {
    const { store, disabled } = this.props;
    const issue = store.getIssue;
    const { labelIssueRelVOList = [] } = issue;

    return (
      <div className="line-start mt-10">
        <div className="c7n-property-wrapper">
          <span className="c7n-property">
            标签
          </span>
        </div>
        <div className="c7n-value-wrapper">
          <TextEditToggle
            disabled={disabled}
            onSubmit={this.updateIssueLabel}
            initValue={this.transToArr(labelIssueRelVOList, 'labelName', 'array')}
            editor={() => <SelectLabel dataRef={this.dataRef} />}
          >
            {
              labelIssueRelVOList.length > 0 ? (
                <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                  {
                    this.transToArr(labelIssueRelVOList, 'labelName', 'array').map(label => (
                      <div
                        key={label}
                        style={{
                          color: '#000',
                          borderRadius: '100px',
                          fontSize: '13px',
                          lineHeight: '24px',
                          padding: '2px 12px',
                          background: 'rgba(0, 0, 0, 0.08)',
                          marginRight: '8px',
                          marginTop: '2px',
                          marginBottom: '2px',
                        }}
                      >
                        {label}
                      </div>
                    ))
                  }
                </div>
              ) : (
                <div>
                  无
                </div>
              )
            }
          </TextEditToggle>
        </div>
      </div>
    );
  }
}

export default withRouter(injectIntl(FieldLabel));
