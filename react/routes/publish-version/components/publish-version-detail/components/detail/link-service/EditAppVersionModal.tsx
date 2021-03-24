import React, {
  useMemo, useCallback, useEffect, useState,
} from 'react';
import {
  DataSet, Form, Modal, Table, TextField,
} from 'choerodon-ui/pro/lib';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import './index.less';
import { IModalProps } from '@/common/types';
import { IAppVersionData, versionApiConfig } from '@/api';
import { observer } from 'mobx-react-lite';
import SelectAppService from '@/components/select/select-app-service';
import SelectGitTags from '@/components/select/select-git-tags';

interface IImportPomFunctionProps {
  handleOk?: ((data: any) => void) | (() => Promise<any>)
  data: IAppVersionData
}

const EditAppVersionModal: React.FC<{ modal?: IModalProps } & Partial<IImportPomFunctionProps>> = ({ modal, handleOk, data }) => {
  const [applicationId, setApplicationId] = useState<string>();
  const formDs = useMemo(() => new DataSet({
    autoCreate: true,
    data: data ? [data] : undefined,
    fields: [
      { name: 'versionAlias', label: '版本别名' },
      { name: 'version', label: 'version' },
      { name: 'artifactId', label: 'artifactId' },
      { name: 'groupId', label: 'groupId' },
      { name: 'appService', label: '关联应用服务' },
      { name: 'tag', label: '关联tag' },
    ],
    transport: {
      submit: data ? ({ data: newData }) => versionApiConfig.updateAppVersion(newData[0], data.id!) : undefined,
    },
  }), [data]);

  const handleSubmit = useCallback(async () => {
    if (!await formDs.submit()) {
      return false;
    }
    const result = handleOk && await handleOk(data);
    return typeof (result) !== 'undefined' ? result : true;
  }, [formDs, handleOk]);
  useEffect(() => {
    modal?.handleOk(handleSubmit);
  }, [handleSubmit, modal]);

  return (
    <Form dataSet={formDs}>
      <TextField name="artifactId" />

      <TextField name="version" />
      <TextField name="versionAlias" />

      <SelectAppService name="appService" onChange={setApplicationId} />
      <SelectGitTags name="tag" applicationId={applicationId} />
    </Form>
  );
};
const ObserverEditAppVersionModal = observer(EditAppVersionModal);
function openEditAppVersionModal(props: IImportPomFunctionProps) {
  const key = Modal.key();
  Modal.open({
    key,
    title: '修改应用版本',
    style: {
      width: MODAL_WIDTH.small,
    },
    drawer: true,
    children: <ObserverEditAppVersionModal {...props} />,
  });
}
function openCreateAppVersionModal() {
  const key = Modal.key();
  Modal.open({
    key,
    title: '创建应用版本',
    style: {
      width: MODAL_WIDTH.small,
    },
    drawer: true,
    children: <ObserverEditAppVersionModal />,
  });
}
export { openEditAppVersionModal, openCreateAppVersionModal };
