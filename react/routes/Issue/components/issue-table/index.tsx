import React, { useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import IssueTable, { IssueTableProps } from '@/components/issue-table';
import useIsInProgram from '@/hooks/useIsInProgram';
import { useUpdateColumnMutation } from '@/hooks/data/useTableColumns';
import { usePersistFn } from 'ahooks';
import { ColumnManage } from '@/components/issue-table/Component';
import getListLayoutColumns from './utils/getListLayoutColumns';

export interface IssueTableMainProps extends IssueTableProps {

}

const IssueTableMain: React.FC<IssueTableMainProps> = ({
  listLayoutColumns: savedListLayoutColumns,
  fields,
  onSummaryClick,
  tableProps,
  onCreateIssue,
  onRowClick,
  tableRef,
  typeIdChange,
  summaryChange,
  IssueStore,
  isTree,
}) => {
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

  return (
    <div>
      <div style={{
        position: 'absolute',
        top: '77px',
        right: '21px',
      }}
      >
        <ColumnManage
          value={visibleColumnCodes}
          onChange={setVisibleColumns}
          options={listLayoutColumns.map(((c) => ({
            code: c.columnCode,
            title: c.columnCode,
          })))}
        />
      </div>
      <IssueTable
        isTree={isTree}
        listLayoutColumns={listLayoutColumns}
        tableProps={tableProps}
        fields={fields}
        tableRef={tableRef}
        onCreateIssue={onCreateIssue}
        onRowClick={onRowClick}
        typeIdChange={typeIdChange}
        summaryChange={summaryChange}
        IssueStore={IssueStore}
        onSummaryClick={onSummaryClick}
        onColumnResize={handleColumnResize}
      />
    </div>

  );
};
export default observer(IssueTableMain);
