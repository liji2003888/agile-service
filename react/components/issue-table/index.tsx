import React, { useState, useMemo, useCallback } from 'react';
import { observer } from 'mobx-react-lite';
import {
  DataSet, PerformanceTable, Pagination,
} from 'choerodon-ui/pro';
import { difference, find } from 'lodash';
import QuickCreateIssue from '@/components/QuickCreateIssue';
import useIsInProgram from '@/hooks/useIsInProgram';
import { useUpdateColumnMutation } from '@/hooks/data/useTableColumns';
import { IField, IIssueColumnName } from '@/common/types';
import { TableProps } from 'choerodon-ui/pro/lib/table/Table';
import './index.less';
import { usePersistFn } from 'ahooks';
import { ListLayoutColumnVO } from '@/api';
import useTable from '@/hooks/useTable';
import { ColumnManage } from './Component';
import { checkBoxColumn, getTableColumns } from './columns';
import transverseTreeData from './utils/transverseTreeData';
import getListLayoutColumns from './utils/getListLayoutColumns';

export interface IssueTableProps extends Partial<TableProps> {
  tableRef?: React.RefObject<any>
  onCreateIssue?: () => void
  dataSet: DataSet
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
  listLayoutColumns: savedListLayoutColumns,
  onSummaryClick,
  selectedIssue,
  createIssue = true,
  typeIdChange = () => { },
  summaryChange = () => { },
  IssueStore,
  tableProps,
  ...otherProps
}) => {
  const handleOpenCreateIssue = useCallback(() => {
    IssueStore?.createQuestion(true);
  }, [IssueStore]);
  const props = tableProps;
  const {
    pagination,
    //  visibleColumns,
    setVisibleColumns, ...restProps
  } = props;
  const mutation = useUpdateColumnMutation('issues.table');

  const listLayoutColumns = useMemo(() => getListLayoutColumns(savedListLayoutColumns, fields), [fields, savedListLayoutColumns]);

  const handleColumnResize = usePersistFn((columnWidth, dataKey) => {
    mutation.mutate({
      applyType: 'agile',
      listLayoutColumnRelVOS: listLayoutColumns.map((listLayoutColumn, i) => ({
        ...listLayoutColumn,
        ...listLayoutColumn.columnCode === dataKey ? {
          width: columnWidth,
        } : {},
      })),
    });
  });
  const { isInProgram } = useIsInProgram();

  const visibleColumnCodes = useMemo(() => (listLayoutColumns.filter((c) => c.display).map((c) => c.columnCode)), [listLayoutColumns]);
  const columns = useMemo(() => getTableColumns({
    listLayoutColumns, fields, onSummaryClick, handleColumnResize,
  }), [fields, handleColumnResize, listLayoutColumns, onSummaryClick]);
  const visibleColumns = useMemo(() => columns.filter((column) => column.display), [columns]);

  const treeData = useMemo(() => transverseTreeData(props.data), [props.data]);
  const checkboxColumn = useMemo(() => checkBoxColumn({
    data: props.data,
    checkValues: props.checkValues,
    handleCheckChange: props.handleCheckChange,
    handleCheckAllChange: props.handleCheckAllChange,
  }), [props.checkValues, props.data, props.handleCheckAllChange, props.handleCheckChange]);

  return (
    <div className="c7nagile-issue-table">
      <ColumnManage
        value={visibleColumnCodes}
        onChange={setVisibleColumns}
        options={listLayoutColumns.map(((c) => ({
          code: c.columnCode,
          title: c.columnCode,
        })))}
      />
      <PerformanceTable
        {...restProps}
        isTree
        rowKey="issueId"
        virtualized
        bordered={false}
        columns={[checkboxColumn, ...visibleColumns]}
        // autoHeight
        height={400}
        data={treeData}
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
      <Pagination
        total={pagination.total}
        page={pagination.current}
        pageSize={pagination.pageSize}
        onChange={pagination.onChange}
      />
    </div>
  );
};
export default observer(IssueTable);
