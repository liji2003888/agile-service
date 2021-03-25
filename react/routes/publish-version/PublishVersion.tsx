import React from 'react';
import {
  TabPage as Page, Header, Content, Breadcrumb,
} from '@choerodon/boot';
import {
  Button, Menu, Table, Tooltip, Modal,
} from 'choerodon-ui/pro';
import { observer } from 'mobx-react-lite';
import {
  omit,
} from 'lodash';
import { ButtonProps } from 'choerodon-ui/pro/lib/button/Button';
import TableDropMenu from '@/common/TableDropMenu';
import { RenderProps } from 'choerodon-ui/pro/lib/field/FormField';
import Record from 'choerodon-ui/pro/lib/data-set/Record';
import { usePublishVersionContext } from './stores';
import { openCreatePublishVersionModal } from './components/create-publish-version';
import { openPublishVersionDetail } from './components/publish-version-detail';

const COLOR_MAP = {
  规划中: '#ffb100',
  已发布: '#00bfa5',
  归档: 'rgba(0, 0, 0, 0.3)',
};
const { Column } = Table;
const TooltipButton: React.FC<{ title?: string } & Omit<ButtonProps, 'title'>> = ({
  title, children, disabled, ...otherProps
}) => {
  if (title && disabled) {
    return <Tooltip title={title}><Button disabled={disabled} {...omit(otherProps, 'onClick')}>{children}</Button></Tooltip>;
  }
  return <Button {...otherProps}>{children}</Button>;
};
function PublishVersion() {
  const { prefixCls, tableDataSet } = usePublishVersionContext();
  function handleClickMenu(key: string, record: Record) {
    switch (key) {
      case 'del':
        Modal.confirm({
          title: '删除发布版本',
          children: (
            <div>
              <span>{`您确定要删除关联的应用版本【${record.get('name')}】？`}</span>
            </div>),
          onOk: () => {

          },
        });
        break;

      default:
        break;
    }
  }
  const renderName = ({ record, text }: RenderProps) => (
    <TableDropMenu
      text={text}
      style={{ lineHeight: '32px' }}
      menu={(
        <Menu onClick={({ key }) => handleClickMenu(key, record!)}>
          <Menu.Item key="publish">发布</Menu.Item>
          <Menu.Item key="export">导出</Menu.Item>
          <Menu.Item key="del">删除</Menu.Item>
        </Menu>
      )}
      onClickEdit={() => openPublishVersionDetail('999')}
    />
  );
  const renderStatus = ({ text }: RenderProps) => (
    <p style={{ marginBottom: 0, minWidth: 60 }}>
      <span
        style={{
          color: '#fff',
          background: COLOR_MAP[text as keyof typeof COLOR_MAP],
          display: 'inline-block',
          lineHeight: '16px',
          height: '16px',
          borderRadius: '2px',
          padding: '0 2px',
          fontSize: '13px',
        }}
      >
        <div style={{ transform: 'scale(.8)' }}>{text === '归档' ? '已归档' : text}</div>
      </span>
    </p>
  );
  return (
    <Page>
      <Header>
        <TooltipButton
          icon="playlist_add"
          title="无相应权限创建发布版本"
          onClick={openCreatePublishVersionModal}
        >
          创建发布版本
        </TooltipButton>
      </Header>
      <Breadcrumb />
      <Content className={`${prefixCls}-content`} style={{ overflowY: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <Table dataSet={tableDataSet}>
          <Column name="name" renderer={renderName} />
          <Column name="status" renderer={renderStatus} />
          <Column name="releaseDate" className="c7n-agile-table-cell" width={110} />
          <Column name="artifactId" className="c7n-agile-table-cell" width={100} />
          <Column name="groupId" className="c7n-agile-table-cell" width={120} />
          <Column name="appService" className="c7n-agile-table-cell" width={120} />
          <Column name="tag" className="c7n-agile-table-cell" width={100} />

        </Table>
      </Content>
    </Page>
  );
}
export default observer(PublishVersion);