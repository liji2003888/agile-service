import React, { useMemo } from 'react';
import {
  Page, Header, Content, Breadcrumb,
} from '@choerodon/boot';
import { Button, Table, DataSet } from 'choerodon-ui/pro';
import { FieldType } from 'choerodon-ui/pro/lib/data-set/enum';
import { statusTransformApiConfig, ITotalStatus } from '@/api';
import StatusTypeTag from '@/components/tag/status-type-tag';
import { IStatus } from '@/common/types';
import { Divider } from 'choerodon-ui';
import { TableAutoHeightType } from 'choerodon-ui/pro/lib/table/enum';
import { TabComponentProps } from '../index';
import openCreateStatus from '../components/create-status';
import openDeleteStatus from './DeleteStatus';
import styles from './index.less';

const { Column } = Table;
const Status: React.FC<TabComponentProps> = ({ tab }) => {
  const dataSet = useMemo(() => new DataSet({
    primaryKey: 'id',
    name: 'status',
    autoQuery: true,
    selection: false,
    transport: {
      read: ({ params }) => statusTransformApiConfig.listStatus(params.page, params.size),
    },
    fields: [
      {
        name: 'name',
        type: 'string' as FieldType,
        label: '名称',
      },
      {
        name: 'type',
        type: 'intl' as FieldType,
        label: '阶段',
      },
      {
        name: 'usage',
        type: 'string' as FieldType,
        label: '使用情况',
      },
      {
        name: 'operate',
        type: 'boolean' as FieldType,
        label: '操作',
      },
    ],
  }), []);
  const handleCreateStatusClick = () => {
    openCreateStatus({
      onSubmit: () => {
        dataSet.query();
      },
    });
  };
  return (
    <Page>
      <Header>
        <Button icon="playlist_add" onClick={handleCreateStatusClick}>创建状态</Button>
      </Header>
      <Breadcrumb />
      <Divider className={styles.divider} />
      <Content>
        {tab}
        <Table
          key="user"
          dataSet={dataSet}
          className={styles.table}
          autoHeight={{
            type: 'maxHeight' as TableAutoHeightType,
            diff: 100,
          }}
        >
          <Column name="name" />
          <Column
            name="type"
            renderer={({ record }) => (
              <StatusTypeTag
                mode="tag"
                code={record?.get('type') as IStatus['valueCode']}
              />
            )}
          />
          <Column name="usage" />
          <Column
            name="operate"
            renderer={({ record }) => (
              <Button
                icon="delete"
                onClick={() => {
                  openDeleteStatus({
                    onSubmit: () => {
                      dataSet.query();
                    },
                    data: record?.toData() as ITotalStatus,
                  });
                }}
              />
            )}
          />
        </Table>
      </Content>
    </Page>
  );
};
export default Status;
