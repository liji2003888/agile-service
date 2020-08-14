import React, { createContext, useMemo, useContext } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import { usePageConfigContext } from '../../stores';
import SchemeTableDataSet from './SchemeTableDataSet';
import useStore from './useStore';

const Store = createContext();
export default Store;

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const contextPageConfig = usePageConfigContext();
    const { objectDetailItem: { schemeCode } } = contextPageConfig;
    const {
      AppState: {
        currentMenuType: { type, id, organizationId },
      }, intl: { formatMessage },
    } = props;
    const store = useStore(type, id, organizationId);
    const schemeTableDataSet = useMemo(() => new DataSet(SchemeTableDataSet({
      projectId: id, formatMessage, organizationId, schemeCode, type,
    }), []));

    const value = {
      ...props,
      prefixCls: 'issue-object-scheme',
      intlPrefix: 'issue-object-scheme',
      store,
      schemeTableDataSet,
      schemeCode,
    };

    return (
      <Store.Provider value={value}>
        {props.children}
      </Store.Provider>
    );
  },
));
