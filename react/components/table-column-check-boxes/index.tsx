/* eslint-disable react/require-default-props */
import React, {
  createContext, useContext, useMemo, useEffect,
} from 'react';
import { injectIntl, InjectedIntl } from 'react-intl';
import {
  CheckBox, DataSet, Form, Table,
} from 'choerodon-ui/pro/lib';
import { observer } from 'mobx-react-lite';
import useIsInProgram from '@/hooks/useIsInProgram';
import { useChoseFieldStore } from '@/components/chose-field/FieldList';
import { IChosenFieldField } from '@/components/chose-field/types';
import ChoseFieldStore from '@/components/chose-field/store';
import { CheckBoxProps } from 'choerodon-ui/pro/lib/check-box/CheckBox';
import { FormProps } from 'choerodon-ui/pro/lib/form/Form';

interface Props<T, TF> {
  options: Array<{ label: string, value: string }>,
  otherCheckBokProps?: T,
  formProps?: TF,
  name?: string,
  dataSet?: DataSet,
  defaultValue?: any,
}
function TableColumnCheckBoxes<T extends Partial<CheckBoxProps>, TF extends Partial<FormProps>>({
  dataSet: propsDataSet, name = 'exportCodes', options, defaultValue, otherCheckBokProps, formProps,
}: Props<T, TF>) {
  const dataSet = useMemo(() => {
    if (propsDataSet) {
      return propsDataSet;
    }
    return new DataSet({
      autoCreate: true,
      autoQuery: false,
      fields: [{
        name, label: '', multiple: true, defaultValue,
      }],
    });
  }, []);
  return (
    <Form dataSet={dataSet} {...formProps}>
      <div>
        {options.map(((option) => (
          <CheckBox name={name} value={option.value} {...otherCheckBokProps}>
            {option.label}
          </CheckBox>
        )))}
      </div>
    </Form>
  );
}
export default observer(TableColumnCheckBoxes);
