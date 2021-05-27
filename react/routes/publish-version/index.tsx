import React from 'react';
import { DataSetProps } from 'choerodon-ui/pro/lib/data-set/DataSet';
import {
  DataSet,
} from 'choerodon-ui/pro';
import Record from 'choerodon-ui/pro/lib/data-set/Record';
import Column from 'choerodon-ui/pro/lib/table/Column';
import { IssueSearchStoreProps } from '@/components/issue-search/store';
import { IPublishVersionData, IPublishVersionTreeNode } from '@/api';
import OriginPublishVersion from './PublishVersion';
import { IPublishVersionContext } from './stores';
import { IPublishVersionBaseDetailStore } from './stores/interface';

interface LinkVersionSectionProps<T> {
  title: React.ReactNode
  className?: string
  bodyClassName?: string
}
interface LinkVersionReturnSectionProps<T> extends Partial<LinkVersionSectionProps<T>> {
  operation: { onClickPom: (data: T) => void, onClickTag: (data: T) => void, onClickVersion: (data: T) => void }
}

export interface IPublishVersionLinkVersionNodeOperationProps {
  isShowEdit: boolean | ((item: IPublishVersionTreeNode) => boolean | undefined)
  isShowDel: boolean | ((item: IPublishVersionTreeNode) => boolean | undefined)
  onEdit?: (newData: any, item: IPublishVersionTreeNode) => any
  onDelete?: (item: IPublishVersionTreeNode) => any
  renderLeftNode?: (item: IPublishVersionTreeNode, node: React.ReactElement) => React.ReactElement
}
interface MenuDetailConfigFieldProps {
  dataKey: string,
  type?: string,
  label: string,
  content?: any
}
interface MenuDetailConfigProps<T> {
  detail: Array<MenuDetailConfigFieldProps>
  linkVersion: { sectionProps: LinkVersionSectionProps<T>, nodeOperationProps: IPublishVersionLinkVersionNodeOperationProps }
}
interface MenuDetailLinkVersionReturnConfigProps<T> {
  sectionProps: LinkVersionReturnSectionProps<T>,
  nodeOperationProps: IPublishVersionLinkVersionNodeOperationProps
}
interface MenuDetailConfigReturnItemProps<DetailType> {
  key: 'detail' | 'linkVersion' | string,
  content: MenuDetailConfigProps<DetailType>['detail'] | MenuDetailLinkVersionReturnConfigProps<DetailType> | React.ReactElement
}
export interface IPublishVersionMenuInfoConfigProps {
  issueSearchStore?: IssueSearchStoreProps // 未实现
  issueInfoDataSetConfig?: DataSetProps // 未实现
  issueInfoColumns?: Array<typeof Column> // 未实现
  issueInfoSwitchCustomOptions?: Array<{ value: string, text: string }>
  issueInfoBody?: React.ReactNode
  onLoadHistory?: () => void
  onLoadTableData?: (optionValue: string) => void
  issueInfoSwitchIssueCountMap?: Map<string, number> // 未实现
}
interface DetailHeaderStatusProps {
  name: string
  color: string
}
export interface IPublishVersionMenuDiffConfigProps {
  topFormContent: React.ReactNode,
  bottomFormContent: React.ReactNode,
  bottomFormProps: { columns: number }
  onCompareTagRequest?: (data: any) => Promise<any>
  onClickSummary?: (data: { issueId: string, projectId?: string }) => void // 未实现
  onChangeIssueTag?: (issueDiffDataSet: DataSet, action: 'add' | 'update') => void
  onSubmitDiff?: (issueDiffDataSet: DataSet) => void // 未实现
}
export interface IPublishVersionProps<DetailType> {
  store?: IPublishVersionBaseDetailStore<DetailType>,
  renderPage?: (pageHeader: React.ReactNode, pageContent: React.ReactNode) => React.ReactElement
  pageHeader?: React.ReactNode
  renderDetailLeftHeader?: (status: (status: DetailHeaderStatusProps) => React.ReactElement) => React.ReactNode
  // pageContent?: React.ReactNode
  pageContentEmpty?: React.ReactElement | ((context: IPublishVersionContext)=>React.ReactElement)
  leftListDataSetConfig?: DataSetProps
  leftListItemConfig?: { renderName: (data: Record) => string, renderMenus: (data: Record) => React.ReactElement, onChange?: (data: Record) => void }
  menuDetail?: (detail: MenuDetailConfigProps<DetailType>['detail'],
    linkVersion: MenuDetailConfigProps<DetailType>['linkVersion']) => Array<MenuDetailConfigReturnItemProps<DetailType>>
  menuInfo?: (context: IPublishVersionContext) => IPublishVersionMenuInfoConfigProps
  menuDiff?: (context: IPublishVersionContext) => IPublishVersionMenuDiffConfigProps | React.ReactElement
  customMenu?: Map<string, { component: React.ReactElement, title: string }>
}
export function ExtendPublishVersion<DetailType>(props: IPublishVersionProps<DetailType>) {
  return <OriginPublishVersion {...props} />;
}
const PublishVersion: React.FC<IPublishVersionProps<IPublishVersionData>> = (props) => <OriginPublishVersion {...props} />;
export default PublishVersion;
