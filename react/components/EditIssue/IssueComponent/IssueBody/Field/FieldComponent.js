import React, { Component } from 'react';
import { observer, inject } from 'mobx-react';
import { withRouter } from 'react-router-dom';
import { injectIntl } from 'react-intl';
import _ from 'lodash';
import { issueApi } from '@/api';
import TextEditToggle from '@/components/TextEditTogglePro';
import SelectComponent from '@/components/select/select-component';


@inject('AppState')
@observer class FieldComponent extends Component {
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

  updateIssueComponents = (newComponents) => {
    const {
      store, onUpdate, reloadIssue, AppState,
    } = this.props;
    const issue = store.getIssue;
    const { issueId, objectVersionNumber, componentIssueRelVOList } = issue;
    const newComponentIssueRelVOList = newComponents || [];
    const originComponents = this.dataRef.current;

    if (JSON.stringify(componentIssueRelVOList) !== JSON.stringify(newComponentIssueRelVOList)) {
      const componentList = [];
      newComponentIssueRelVOList.forEach((component) => {
        const target = _.find(originComponents, { name: component });
        if (target) {
          componentList.push(target);
        } else {
          componentList.push({
            name: component,
            projectId: AppState.currentMenuType.id,
          });
        }
      });
      const obj = {
        issueId,
        objectVersionNumber,
        componentIssueRelVOList: componentList,
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
    const { componentIssueRelVOList = [] } = issue;
    return (
      <div className="line-start mt-10">
        <div className="c7n-property-wrapper">
          <span className="c7n-property">
            模块
          </span>
        </div>
        <div className="c7n-value-wrapper">
          <TextEditToggle
            disabled={disabled}
            onSubmit={this.updateIssueComponents}
            initValue={this.transToArr(componentIssueRelVOList, 'name', 'array')}
            editor={(
              <SelectComponent
                dataRef={this.dataRef}
                getPopupContainer={() => document.getElementById('detail')} 
                style={{ marginTop: 0, paddingTop: 0 }}
              />
          )}
          >
            {componentIssueRelVOList && componentIssueRelVOList.length
              ? (
                <div>
                  <p className="primary" style={{ wordBreak: 'break-word', marginTop: 2 }}>
                    {this.transToArr(componentIssueRelVOList, 'name', 'array').join(',')}
                  </p>
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

export default withRouter(injectIntl(FieldComponent));
