import React, { useMemo, forwardRef } from 'react';
import { Select } from 'choerodon-ui/pro';
import { Tooltip } from 'choerodon-ui';
import { find } from 'lodash';
import { issueTypeApi, sprintApi } from '@/api';
import useSelect, { SelectConfig } from '@/hooks/useSelect';
import { SelectProps } from 'choerodon-ui/pro/lib/select/Select';
import { IIssueType, ISprint } from '@/common/types';
import { FlatSelect } from '@choerodon/components';

interface Props extends Partial<SelectProps> {
  filterList?: string[]
  request?: SelectConfig<any>['request']
  isProgram?: boolean
  afterLoad?: (sprints: IIssueType[]) => void
  dataRef?: React.MutableRefObject<any>
  valueField?: string
  flat?: boolean
  projectId?:string
  applyType?:string
}

const SelectIssueType: React.FC<Props> = forwardRef(({
  filterList = ['feature'], isProgram, request, valueField, dataRef, flat,
  afterLoad, projectId, applyType, ...otherProps
}, ref: React.Ref<Select>) => {
  const config = useMemo((): SelectConfig<IIssueType> => ({
    name: 'issueType',
    textField: 'name',
    valueField: valueField || 'id',
    request: request || (() => issueTypeApi.loadAllWithStateMachineId(applyType ?? (isProgram ? 'program' : undefined), projectId).then((issueTypes) => {
      if (isProgram) {
        const featureTypes: any = [{
          id: 'business',
          name: '特性',
          colour: '',
          description: '',
          icon: '',
          stateMachineId: '',
          typeCode: '',
        }, {
          id: 'enabler',
          name: '使能',
          colour: '',
          description: '',
          icon: '',
          stateMachineId: '',
          typeCode: '',
        }];
        const epicType: IIssueType = find<IIssueType>(issueTypes, { typeCode: 'issue_epic' }) as IIssueType;
        return [...featureTypes, epicType];
      }
      if (Array.isArray(filterList) && filterList.length > 0) {
        return issueTypes.filter((issueType) => !filterList.some((filter) => filter === issueType.typeCode));
      }
      return issueTypes;
    })),
    middleWare: (issueTypes) => {
      if (afterLoad) {
        afterLoad(issueTypes);
      }
      if (dataRef) {
        Object.assign(dataRef, {
          current: issueTypes,
        });
      }
      return issueTypes;
    },
    paging: false,
  }), []);
  const props = useSelect(config);
  const Component = flat ? FlatSelect : Select;

  return (
    <Component
      ref={ref}
      {...props}
      {...otherProps}
    />
  );
});
export default SelectIssueType;
