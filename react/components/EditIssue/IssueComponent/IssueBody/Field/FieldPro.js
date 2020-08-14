import React, { Component } from 'react';
import { observer } from 'mobx-react';
import moment from 'moment';
import SelectUser from '@/components/select/select-user';
import {
  TextField, TextArea, Select, NumberField, DatePicker, DateTimePicker, TimePicker, SelectBox,
} from 'choerodon-ui/pro';
import { fieldApi } from '@/api';
import TextEditToggle from '@/components/TextEditTogglePro';
import UserHead from '@/components/UserHead';

const EditorMap = new Map([
  ['text', TextArea],
  ['input', TextField],
  ['member', SelectUser],
  ['single', Select],
  ['multiple', Select],
  ['radio', SelectBox],
  ['checkbox', SelectBox],
  ['number', NumberField],
  ['time', TimePicker],
  ['date', DatePicker],
  ['datetime', DateTimePicker],
]);
@observer class FieldPro extends Component {
  updateIssueField = (value) => {
    const {
      store, onUpdate, reloadIssue, field,
    } = this.props;
    const issue = store.getIssue;
    const {
      fieldId, fieldType,
    } = field;
    let newValue = value;
    if (fieldType === 'time' || fieldType === 'datetime' || fieldType === 'date') {
      newValue = value && value.format('YYYY-MM-DD HH:mm:ss');
    }
    const { issueId } = issue;
    const obj = {
      fieldType,
      value: newValue,
    };
    fieldApi.updateFieldValue(issueId, fieldId, 'agile_issue', obj)
      .then(() => {
        if (onUpdate) {
          onUpdate();
        }
        if (reloadIssue) {
          reloadIssue(issueId);
        }
      });
  };

  transform = (fieldType, value) => {
    if (fieldType === 'time' || fieldType === 'datetime' || fieldType === 'date') {
      return value ? moment(value) : undefined;
    } else if (value instanceof Array) {
      return value.slice();
    } else {
      return value;
    }
  };

  renderEditor = () => {
    const { field } = this.props;
    const { value, fieldType, required } = field;
    const Editor = EditorMap.get(fieldType);

    if (Editor) {
      switch (fieldType) {
        case 'single':
        case 'multiple':
        case 'radio':
        case 'checkbox':
        {
          const options = field.fieldOptions && field.fieldOptions.length > 0
              && field.fieldOptions.filter(option => option.enabled || (value && value.indexOf(option.id) !== -1)).map(item => (
                <Editor.Option
                  value={item.id}
                  key={item.id}
                >
                  {item.value}
                </Editor.Option>
              ));
          return (
            <Editor vertical required={required} multiple={fieldType === 'multiple' || fieldType === 'checkbox'}>
              {options}
            </Editor>
          );
        }
        case 'text': {
          return <Editor required={required} autoSize />;
        }
        default: return <Editor required={required} />;
      }
    }
    return null;
  }

  render() {
    const { field, disabled } = this.props;
    const {
      fieldName, value, fieldType, valueStr,
    } = field;
    const submitTrigger = ['blur'];
    const submitOnChange = ['member', 'single', 'radio'].includes(fieldType);
    if (submitOnChange) {
      submitTrigger.push('change');
    }
    const submitOnOut = ['radio', 'checkbox'].includes(fieldType);
    if (submitOnOut) {
      submitTrigger.push('click');
    }
    return (
      <div className="line-start mt-10">
        <div className="c7n-property-wrapper">
          <span className="c7n-property">
            {`${fieldName}`}
          </span>
        </div>
        <div className="c7n-value-wrapper" style={{ width: 'auto' }}>
          <TextEditToggle
            disabled={disabled}
            alwaysRender={!['time', 'date', 'datetime'].includes(fieldType)}
            onSubmit={this.updateIssueField}
            initValue={this.transform(fieldType, value)}
            editor={this.renderEditor}
            submitTrigger={submitTrigger}
          >
            <div style={{ maxWidth: 200, wordBreak: 'break-all', whiteSpace: 'pre-line' }}>
              {fieldType === 'member' && valueStr
                ? (
                  <UserHead
                    user={valueStr}
                  />
                ) : (valueStr || '无')}
            </div>
          </TextEditToggle>
        </div>
      </div>
    );
  }
}

export default FieldPro;
