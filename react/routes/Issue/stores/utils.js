import { toJS } from 'mobx';

function transformSystemFilter(data) {
  const {
    issueTypeId,
    assigneeId,
    statusId,
    priorityId,
    issueIds,
    quickFilterIds,
    createDate = [],
    updateDate = [],
    estimatedStartTime = [],
    estimatedEndTime = [],
    contents,
    component,
    epic,
    feature,
    label,
    reporterIds,
    sprint,
    summary,
    version,
    fixVersion,
    influenceVersion,
    starBeacon,
    userId,
    testResponsibleIds,
    mainResponsibleIds,
    creatorIds,
    updatorIds,
    environment,
    appVersion,
  } = data;
  return {
    advancedSearchArgs: {
      issueTypeId,
      reporterIds,
      statusId,
      priorityId,
    },
    otherArgs: {
      userId,
      starBeacon,
      assigneeId,
      issueIds,
      component,
      epic,
      feature,
      label,
      sprint,
      summary,
      version,
      fixVersion,
      influenceVersion,
      testResponsibleIds,
      mainResponsibleIds,
      environment,
      creatorIds,
      updatorIds,
      appVersion,
    },
    searchArgs: {
      estimatedStartTimeScopeStart: estimatedStartTime[0],
      estimatedStartTimeScopeEnd: estimatedStartTime[1],
      estimatedEndTimeScopeStart: estimatedEndTime[0],
      estimatedEndTimeScopeEnd: estimatedEndTime[1],
      createStartDate: createDate[0],
      createEndDate: createDate[1],
      updateStartDate: updateDate[0],
      updateEndDate: updateDate[1],
    },
    quickFilterIds,
    contents,
  };
}
export function transformFilter(chosenFields) {
  const customField = {
    option: [],
    date: [],
    date_hms: [],
    number: [],
    string: [],
    text: [],
  };
  const systemFilter = {};
  for (const [code, field] of chosenFields) {
    const { fieldType, id } = field;
    const value = toJS(field.value);
    if (value === undefined || value === null || value === '') {
      // eslint-disable-next-line no-continue
      continue;
    }
    // 系统字段
    if (!id) {
      systemFilter[code] = value;
      // eslint-disable-next-line no-continue
      continue;
    }
    switch (fieldType) {
      case 'single':
      case 'multiple':
      case 'radio':
      case 'checkbox':
      case 'multiMember':
      case 'member': {
        const v = Array.isArray(value) ? value : [value];
        if (v.length > 0) {
          customField.option.push({
            fieldId: id,
            value: v,
          });
        }
        break;
      }
      case 'input': {
        if (value && value.length > 0) {
          customField.string.push({
            fieldId: id,
            value,
          });
        }
        break;
      }
      case 'text': {
        if (value && value.length > 0) {
          customField.text.push({
            fieldId: id,
            value,
          });
        }
        break;
      }
      case 'number': {
        customField.number.push({
          fieldId: id,
          value,
        });
        break;
      }
      case 'time':
      case 'datetime':
      case 'date': {
        if (value && value.length > 0) {
          if (fieldType === 'time') {
            customField.date_hms.push({
              fieldId: id,
              startDate: value[0],
              endDate: value[1],
            });
          } else {
            customField.date.push({
              fieldId: id,
              startDate: value[0],
              endDate: value[1],
            });
          }
        }
        break;
      }
      default: break;
    }
  }
  const filter = transformSystemFilter(systemFilter);
  filter.otherArgs.customField = customField;
  return filter;
}
