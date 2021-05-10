import React, { useState, useMemo, useCallback } from 'react';
import { observer } from 'mobx-react-lite';
import {
  PerformanceTable, Pagination,
} from 'choerodon-ui/pro';
import QuickCreateIssue from '@/components/QuickCreateIssue';
import { IField, IIssueColumnName } from '@/common/types';
import { TableProps } from 'choerodon-ui/pro/lib/table/Table';
import './index.less';
import { ListLayoutColumnVO } from '@/api';
import useTable from '@/hooks/useTable';
import { checkBoxColumn, getTableColumns } from './columns';

export interface IssueTableProps extends Partial<TableProps> {
  tableRef?: React.RefObject<any>
  onCreateIssue?: () => void
  // dataSet: DataSet
  fields: IField[]
  onRowClick?: (record: any) => void
  selectedIssue?: string
  createIssue?: boolean
  visibleColumns?: IIssueColumnName[]
  listLayoutColumns: ListLayoutColumnVO[] | null
  onSummaryClick: () => void
  typeIdChange?: (id: string) => void
  summaryChange?: (summary: string) => void
  IssueStore?: any
  tableProps: ReturnType<typeof useTable>
  onColumnResize?: (columnWidth: number, dataKey: string) => void
  isTree?: boolean
  height?:number
}
// const mapper = (key: IIssueColumnName): string => ({
//   summary: 'issueId',
//   issueNum: 'issueNum',
//   priority: 'priorityId',
//   sprint: 'issueSprintVOS',
//   reporter: 'reporterId',
//   creationDate: 'creationDate',
//   assign: 'assigneeId',
//   status: 'statusId',
//   lastUpdateDate: 'lastUpdateDate',
//   estimatedStartTime: 'estimatedStartTime',
//   estimatedEndTime: 'estimatedEndTime',
//   label: 'label',
//   component: 'component',
//   storyPoints: 'storyPoints',
//   fixVersion: 'fixVersion',
//   influenceVersion: 'influenceVersion',
//   epic: 'epic',
//   feature: 'feature',
// }[key] || key);

const IssueTable: React.FC<IssueTableProps> = ({
  tableRef,
  onCreateIssue,
  dataSet,
  fields,
  listLayoutColumns,
  onSummaryClick,
  selectedIssue,
  createIssue = true,
  typeIdChange = () => { },
  summaryChange = () => { },
  IssueStore,
  tableProps,
  onColumnResize,
  isTree = true,
  height,
  ...otherProps
}) => {
  const handleOpenCreateIssue = useCallback(() => {
    IssueStore?.createQuestion(true);
  }, [IssueStore]);
  const props = tableProps;
  const {
    pagination,
    ...restProps
  } = props;

  const columns = useMemo(() => getTableColumns({
    listLayoutColumns, fields, onSummaryClick, handleColumnResize: onColumnResize,
  }), [fields, listLayoutColumns, onColumnResize, onSummaryClick]);
  const visibleColumns = useMemo(() => columns.filter((column) => column.display), [columns]);

  const checkboxColumn = useMemo(() => checkBoxColumn({
    data: props.data,
    checkValues: props.checkValues,
    handleCheckChange: props.handleCheckChange,
    handleCheckAllChange: props.handleCheckAllChange,
  }), [props.checkValues, props.data, props.handleCheckAllChange, props.handleCheckChange]);

  return (
    <div className="c7nagile-issue-table">
      <PerformanceTable
        {...restProps}
        virtualized
        bordered={false}
        columns={[checkboxColumn, ...visibleColumns]}
        height={height ?? 400}
      />
      {createIssue && (
        <div style={{ paddingTop: 5 }}>
          <QuickCreateIssue
            onCreate={onCreateIssue}
            cantCreateEvent={handleOpenCreateIssue}
            typeIdChange={typeIdChange}
            summaryChange={summaryChange}
          />
        </div>
      )}
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Pagination
          total={pagination.total}
          page={pagination.current}
          pageSize={pagination.pageSize}
          onChange={pagination.onChange}
        />
      </div>

    </div>
  );
};
export default observer(IssueTable);
