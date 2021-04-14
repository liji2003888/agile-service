import { IModalProps } from '@/common/types';
import {
  DataSet, Form, Modal,
} from 'choerodon-ui/pro';
import React, { useCallback, useEffect, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import { versionApiConfig } from '@/api';
import { getProjectId } from '@/utils/common';
import SelectProgramVersion from '@/components/select/select-program-version';

interface Props {
  modal?: IModalProps,
  programId: string
  versionId: string
  defaultValue?: any
  onRefresh: () => void
}
const LinkProgramVersion: React.FC<Props> = (props) => {
  const ds = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [{
      name: 'programVersion',
      label: '项目群版本',
    }],
    transport: {
      submit: ({ data, params }) => ({
        ...data[0].programVersion ? versionApiConfig.linkProgramVersion(data[0].programVersion, props.versionId)
          : versionApiConfig.deleteLinkProgramVersion(props.defaultValue, props.versionId),
        data: null,
      }),
    },
  }), [props.defaultValue, props.versionId]);

  const handleOnOk = useCallback(async () => {
    if (!ds.current?.dirty) {
      return true;
    }
    if (await ds.current?.validate()) {
      await ds.submit();
      props.onRefresh && props.onRefresh();
      return true;
    }
    return false;
  }, [ds, props]);
  useEffect(() => {
    props.modal?.handleOk(handleOnOk);
  }, []);
  return (
    <Form dataSet={ds}>
      <SelectProgramVersion
        name="programVersion"
        teamProjectIds={[getProjectId()]}
        clearButton
        optionFlat
        afterLoad={() => {
          props.defaultValue && ds.current?.init('programVersion', props.defaultValue);
        }}
      />
    </Form>
  );
};
const ObserverLinkProgramVersion = observer(LinkProgramVersion);
const openLinkVersionModal = (versionId: string, programId: string, programVersion: { name: string, id: string } | undefined, onRefresh: () => void) => {
  Modal.open({
    key: 'LinkPiAimModal',
    title: '关联项目群版本',
    drawer: true,
    style: {
      width: MODAL_WIDTH.small,
    },
    children: <ObserverLinkProgramVersion versionId={versionId} programId={programId} onRefresh={onRefresh} defaultValue={programVersion?.id} />,
  });
};
export { openLinkVersionModal };
