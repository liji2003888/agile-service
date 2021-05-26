import { fieldApi } from '@/api';
import SelectUser from '@/components/select/select-user';
import UserTag from '@/components/tag/user-tag';
import TextEditToggle from '@/components/TextEditTogglePro';
import {
  DatePicker, DateTimePicker, NumberField, Select, SelectBox, TextArea, TextField, TimePicker,
} from 'choerodon-ui/pro';
import { observer } from 'mobx-react';
import moment from 'moment';
import { MAX_NUMBER_VALUE, MAX_NUMBER_STEP } from '@/constants/MAX_VALUE';
import React, { Component } from 'react';
import SelectCustomField from '@/components/select/select-custom-field';

const EditorMap = new Map([
  ['text', TextArea],
  ['input', TextField],
  ['member', SelectUser],
  ['single', SelectCustomField],
  ['multiple', SelectCustomField],
  ['radio', SelectBox],
  ['checkbox', SelectBox],
  ['number', NumberField],
  ['time', TimePicker],
  ['date', DatePicker],
  ['datetime', DateTimePicker],
  ['multiMember', SelectUser],

]);
@observer class FieldPro extends Component {
  updateIssueField = (value) => {
    const {
      store, onUpdate, reloadIssue, field, setIssueLoading,
    } = this.props;
    const issue = store.getIssue;
    const {
      fieldId, fieldType, fieldCode,
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
    setIssueLoading(true);
    fieldApi.updateFieldValue(issueId, fieldId, fieldCode, 'agile_issue', obj)
      .then(() => {
        if (onUpdate) {
          onUpdate();
        }
        if (reloadIssue) {
          reloadIssue(issueId);
        }
      }).catch(() => {
        setIssueLoading(false);
      });
  };

  transform = (fieldType, value) => {
    if (fieldType === 'time' || fieldType === 'datetime' || fieldType === 'date') {
      return value ? moment(value) : undefined;
    } if (value instanceof Array) {
      return value.slice();
    }
    return value;
  };

  renderEditor = () => {
    const { field } = this.props;
    const {
      value, fieldType, required, valueStr, extraConfig, fieldId,
    } = field;
    const Editor = EditorMap.get(fieldType);
    if (Editor) {
      switch (fieldType) {
        case 'single':
        case 'multiple':
        {
          return (
            <Editor
              searchable
              selected={value}
              required={required}
              fieldId={fieldId}
              multiple={fieldType === 'multiple'}
            />
          );
        }
        case 'radio':
        case 'checkbox':
        {
          const options = field.fieldOptions && field.fieldOptions.length > 0
              && field.fieldOptions.filter((option) => option.enabled
                || (value && value.indexOf(option.id) !== -1)).map((item) => (
                  <Editor.Option
                    value={item.id}
                    key={item.id}
                  >
                    {item.value}
                  </Editor.Option>
              ));
          return (
            <Editor
              vertical
              searchable={fieldType === 'multiple' || fieldType === 'single'}
              required={required}
              multiple={fieldType === 'multiple' || fieldType === 'checkbox'}
            >
              {options}
            </Editor>
          );
        }
        case 'text': {
          return <Editor required={required} autoSize />;
        }
        case 'multiMember': {
          return <Editor required={required} multiple selectedUser={valueStr} />;
        }
        case 'member': {
          return <Editor required={required} selectedUser={valueStr} />;
        }
        case 'number': {
          return <Editor required={required} max={MAX_NUMBER_VALUE} step={extraConfig ? MAX_NUMBER_STEP : 1} />;
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
              {['member', 'multiMember'].includes(fieldType) && valueStr
                ? <UserTag data={valueStr} /> : (valueStr || '无')}
            </div>
          </TextEditToggle>
        </div>
      </div>
    );
  }
}

export default FieldPro;
