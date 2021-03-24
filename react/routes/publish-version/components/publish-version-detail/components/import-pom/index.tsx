import React, {
  useMemo, useRef, useState, useCallback, useEffect,
} from 'react';
import {
  DataSet, Form, Modal, Select, Spin, Table, TextField, Tooltip,
} from 'choerodon-ui/pro/lib';
import { Button } from 'choerodon-ui';
import { Choerodon } from '@choerodon/boot';
import classnames from 'classnames';
import MODAL_WIDTH from '@/constants/MODAL_WIDTH';
import './index.less';
import { RenderProps } from 'choerodon-ui/pro/lib/field/FormField';
import SelectTeam from '@/components/select/select-team';
import { IModalProps } from '@/common/types';
import { versionApi } from '@/api';
import Loading from '@/components/Loading';
import { observer } from 'mobx-react-lite';
import Record from 'choerodon-ui/pro/lib/data-set/Record';

interface IImportPomFunctionProps {
  handleOk?: ((data: any) => void) | (() => Promise<any>)
  programMode?: boolean
}

const { Column } = Table;
const ImportPom: React.FC<{ modal?: IModalProps } & IImportPomFunctionProps> = ({ modal, handleOk, programMode }) => {
  const prefixCls = 'c7n-agile-release-detail-import-pom';
  const [groupId, setGroupId] = useState<string[] | undefined>();
  const [subProjectId, setSubProjectId] = useState<string>();
  const [loading, setLoading] = useState<boolean>(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const formDs = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [
      { name: 'subProject', label: '所属项目', required: programMode },
      { name: 'groupId', label: 'GroupId' },
    ],
  }), [programMode]);
  const disabledUpload = useMemo(() => {
    if (programMode && formDs.current?.get('subProject')) {
      return false;
    }
    return !!programMode;
  }, [formDs, formDs.current?.get('subProject')]);

  const ds = useMemo(() => new DataSet({
    autoQuery: false,
    paging: false,
    selection: false,

    // data: [
    //   { appService: '应用1', version: '0.18.a', alias: undefined },
    // ],
    fields: [
      { name: 'artifactId', label: '应用服务' },
      { name: 'version', label: '版本名称', required: true },
      { name: 'versionAlias', label: '版本别名' },
      { name: 'appService', label: '主服务' },
    ],
  }), []);
  const handleUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      console.log('groupId', groupId);
      const file = e.target.files[0];
      if (!file) {
        Choerodon.prompt('请选择文件');
        return;
      }
      const formData = new FormData();
      formData.append('file', file);
      setLoading(true);
      const groupIdStr = groupId ? String(groupId) : undefined;
      (programMode ? versionApi.importProgramPom(formData, groupIdStr!, subProjectId!) : versionApi.importPom(formData, groupIdStr!)).then((res: any) => {
        ds.loadData(res);
        // ds.splice(0,0,)
      }).finally(() => {
        setLoading(false);
        inputRef.current?.setAttribute('value', '');
      });

      // issueApi.import(formData).then((res) => {

      // }).catch((e) => {

      //   Choerodon.prompt('网络错误');
      // });
    }
  };
  const handleSubmit = useCallback(async () => {
    console.log('submit');
    const versionCheckRes = Promise.all(ds.map((r) => r.getField('version')?.checkValidity())).then((v) => {
      console.log('iii', v);
      return !v.some((i) => !i);
    });
    if (!await versionCheckRes) {
      return false;
    }

    const data = ds.toData();
    const result = handleOk && await handleOk(data);
    return typeof (result) !== 'undefined' ? result : true;
  }, [ds, handleOk]);
  useEffect(() => {
    modal?.handleOk(handleSubmit);
  }, [handleSubmit, modal]);
  function renderAlias({ value }: RenderProps) {
    if (!value) {
      return <i className={`${prefixCls}-table-alias-no-value`}>请输入别名</i>;
    }
    return value;
  }
  function renderAction({ record }: RenderProps) {
    return record?.get('appService') ? undefined : <Button icon="delete_forever" onClick={() => ds.delete(record!, false)} />;
  }
  return (
    <div className={prefixCls}>
      <Form dataSet={formDs} style={{ width: '6.2rem' }}>
        {programMode ? <SelectTeam name="subProject" onChange={setSubProjectId} /> : null}

        <TextField name="groupId" onChange={setGroupId} multiple />
      </Form>
      <div className={`${prefixCls}-body`}>
        <div className={`${prefixCls}-upload`}>
          <span>上传pom文件</span>
          <Tooltip title={disabledUpload ? `请先选择${programMode ? '和所属项目' : ''}` : undefined}>
            <Button disabled={disabledUpload} funcType="raised" size={'small' as any} type="primary" style={{ color: !disabledUpload ? 'white' : undefined }} icon="file_upload" shape="circle" onClick={() => inputRef.current?.click()} />
          </Tooltip>
        </div>

        <Table dataSet={ds} queryBar={'none' as any}>
          <Column name="artifactId" tooltip={'overflow' as any} />
          <Column name="version" editor />
          <Column name="versionAlias" editor renderer={renderAlias} tooltip={'overflow' as any} />
          <Column name="appService" renderer={({ value }) => (value ? '是' : '否')} width={90} />

          <Column name="action" renderer={renderAction} width={65} />
        </Table>
      </div>
      <input
        ref={inputRef}
        type="file"
        onChange={handleUpload}
        style={{ display: 'none' }}
      // accept="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      />
      <Loading loading={loading} />
    </div>
  );
};
const ObserverImportPom = observer(ImportPom);
function openImportPomModal(props: IImportPomFunctionProps) {
  const key = Modal.key();
  Modal.open({
    key,
    title: '导入pom文件',
    style: {
      width: MODAL_WIDTH.middle,
    },
    className: classnames('c7n-agile-export-issue-modal'),
    drawer: true,
    children: <ObserverImportPom {...props} />,
  });
}
export { openImportPomModal };
