import React, { useEffect, useRef, useState } from 'react';
import {
  Button, Icon, Modal, SelectBox, Tooltip,
} from 'choerodon-ui/pro/lib';
import { pick } from 'lodash';
import { Tree } from 'choerodon-ui/pro';
import classnames from 'classnames';
import { ButtonColor } from 'choerodon-ui/pro/lib/button/enum';
import { randomString } from '@/utils/random';
import './DependencyTreeBase.less';

interface IDependencyTreeNodeBaseProps<T> {
  id: string
  children?: Array<T>
}
interface IDependencyTreeProps<T extends IDependencyTreeNodeBaseProps<T>> {
  data: Array<T>
  renderNode: (data: T, level: number) => React.ReactElement
}
const { TreeNode } = Tree;
/**
 * 带有连线的tree
 *
 * @returns
 */
function DependencyTree<T extends IDependencyTreeNodeBaseProps<T>>({ data: propsData, renderNode }: IDependencyTreeProps<T>) {
  const prefixCls = 'c7n-agile-dependency-tree';
  const [data, setData] = useState(propsData || []);
  useEffect(() => {
    setData(propsData);
  }, [propsData]);
  function renderTreeNodeLine(hasChildren: boolean, nodeIndex: number, level: number, needLineLevelList: number[], height = 30) {
    const lineArr = [];
    lineArr.push(<span
      className={`${prefixCls}-expand-line`}
      style={{
        right: hasChildren ? 17 : undefined,
        bottom: hasChildren ? 5 : undefined,
        width: hasChildren ? 16 : undefined,
        // height: 30,
        height: nodeIndex !== 0 ? height : undefined,
      }}
    />);
    needLineLevelList.forEach((item) => {
      const right = 24 * (level - item + 1) - 8 + (hasChildren ? 16 : 0);
      const width = 1;
      // if (index !== level) {
      //   width = 0;
      // }
      lineArr.push(<span
        className={`${prefixCls}-expand-line ${prefixCls}-expand-line${item} level${level}`}
        style={{
          // display: index !== level ? 'none' : undefined,
          // right: hasChildren ? 16 * level : undefined,
          // bottom: hasChildren ? 10 - 5 * level : undefined,
          right,
          height,
          bottom: hasChildren ? 5 : undefined,
          width,
        }}
      />);
    });
    // for (let index = 2; index < level + 1; index += 1) {
    //   const right = 24 * index - 8 + (hasChildren ? 16 : 0);
    //   const width = 1;
    //   // if (index !== level) {
    //   //   width = 0;
    //   // }
    //   index === level && lineArr.push(<span
    //     className={`${prefixCls}-expand-line`}
    //     style={{
    //       // display: index !== level ? 'none' : undefined,
    //       // right: hasChildren ? 16 * level : undefined,
    //       // bottom: hasChildren ? 10 - 5 * level : undefined,
    //       right,
    //       height: 30,
    //       bottom: hasChildren ? 5 : undefined,
    //       width,
    //     }}
    //   />);
    // }
    return lineArr;
  }
  function renderTreeNode(item: T, level: number) {
    const node = renderNode(item, level);
    const nodeStyle = pick(node.props, 'style') || {};
    return React.cloneElement(node, {
      ...node.props,
      style: {
        ...nodeStyle,
        // minHeight: 30,
        // height: 30,
      },
    });
  }
  function renderTree(item: T, level = 0, index = 0, parentHasNextBrotherNode = false, needLineLevelList: number[] = []) {
    // let paddingLeft = level === 0 ? undefined : (28 * level - 20 * (level - 1));
    // if (!!item.children?.length && level > 1 && paddingLeft && paddingLeft > 0) {
    //   paddingLeft -= 11;
    // }
    const node = renderNode(item, level);
    const { style: nodeStyle } = pick(node.props, 'style') || {};
    return (
      <TreeNode
        title={node}
        key={`${randomString(5)}-level-${level}-${item.id}`}
        disabled
        className={classnames({
          [`${prefixCls}-root`]: !level,
          [`${prefixCls}-leaf`]: !item.children?.length,
          [`${prefixCls}-has-leaf`]: !!item.children?.length,
        })}
        style={{
          paddingLeft: 0,
          marginLeft: 0,
        }}
        switcherIcon={(nodeProps: any) => (
          <div className={`${prefixCls}-expand`}>
            {level !== 0 && renderTreeNodeLine(!!item.children?.length, index, level, needLineLevelList, nodeStyle.height)}
            {item.children?.length ? <Icon type="navigate_next" className={`${prefixCls}-expand-icon`} /> : null}
          </div>
        )}
      >
        {item.children?.map((k, nodeIndex) => {
          const newNeedLineLevelList: number[] = [];
          newNeedLineLevelList.push(...needLineLevelList);
          parentHasNextBrotherNode && newNeedLineLevelList.push(level); // 父节节点有下一个兄弟节点
          return renderTree(k, level + 1, nodeIndex, nodeIndex + 1 !== item.children!.length, newNeedLineLevelList);
        })}
      </TreeNode>
    );
  }

  return (
    <Tree className={`${prefixCls}`}>
      {data?.map((item, index) => renderTree(item, 0, index, false, []))}
    </Tree>
  );
}
export default DependencyTree;
