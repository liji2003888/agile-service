import React, { useCallback, ReactNode } from 'react';
import { Icon } from 'choerodon-ui';
import styles from './Table.less';

interface Column<T = {}> {
  title: string
  dataIndex: string
  render?: (record: T) => React.ReactNode
}
interface Props<T> {
  columns: Column<T>[]
  data: T[]
  primaryKey: string
}
export function createColumns<T extends {}>(columns: Column<T>[]): Column<T>[] {
  return columns;
}
function Table<T extends { [key: string]: any }>({
  columns,
  data,
  primaryKey,
}: Props<T>) {
  const renderTreeLikeData = useCallback((treeData: T[], level: number = 0): ReactNode => treeData.map((item) => {
    const hasChildren = item.children
    && item.children instanceof Array
    && item.children.length > 0;
    return (
      <>
        <tr key={item[primaryKey]} data-level={level}>
          {columns.map((column, index) => (
            <td>
              {index === 0 && <span style={{ display: 'inline-block', width: 10 + level * 20 }} />}
              {index === 0 && hasChildren && <Icon type="arrow_drop_down" />}
              {column.render
                ? column.render(item) : item[column.dataIndex]}
            </td>
          ))}
        </tr>
        { hasChildren && renderTreeLikeData(item.children, level + 1)}
      </>
    );
  }), [columns, primaryKey]);
  return (
    <table className={styles.table}>
      <thead>
        <tr>
          {columns.map((column) => <th key={column.dataIndex}>{column.title}</th>)}
        </tr>
      </thead>
      <tbody>
        {renderTreeLikeData(data)}
      </tbody>
    </table>
  );
}
export default Table;
