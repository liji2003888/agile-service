import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import { Icon, Popconfirm, Tooltip } from 'choerodon-ui';
import './DocItem.less';


class DocItem extends Component {
  paramConverter = (url) => {
    const reg = /[^?&]([^=&#]+)=([^&#]*)/g;
    const retObj = {};
    url.match(reg).forEach((item) => {
      const [tempKey, paramValue] = item.split('=');
      const paramKey = tempKey[0] !== '&' ? tempKey : tempKey.substring(1);
      Object.assign(retObj, {
        [paramKey]: paramValue,
      });
    });
    return retObj;
  };

  getUrl = (id) => {
    const { origin } = window.location;
    const { location } = this.props;
    const { search } = location;
    const params = this.paramConverter(search);
    return `${origin}#/knowledge/project?spaceId=${id}&type=project&id=${params.id}&name=${params.name}&category=${params.category}&organizationId=${params.organizationId}&orgId=${params.organizationId}`;
  };

  handleDocClick = ({ id, baseId }) => {
    const { location, history } = this.props;
    const { search } = location;
    const params = this.paramConverter(search);
    history.push(`/knowledge/project/doc/${baseId}?spaceId=${id}&type=project&id=${params.id}&name=${params.name}&category=${params.category}&organizationId=${params.organizationId}&orgId=${params.organizationId}`);
  };

  render() {
    const {
      doc, type, onDeleteDoc,
    } = this.props;
    return (
      <div
        className="c7n-docItem"
      >
        <Icon type="filter_none" className="c7n-docItem-icon" />
        {doc.workSpaceVO
          ? (
            <a
              role="none"
              className={`c7n-docItem-text c7n-docItem-${type}`}
              onClick={this.handleDocClick.bind(this, doc.workSpaceVO)}    
              rel="noopener noreferrer"
            >
              {doc.workSpaceVO.name}
            </a>
          )
          : (
            <Tooltip title="关联的知识文档已被删除，请手动删除此关联。">
              <span style={{ color: 'red' }}>
                {doc.wikiName}
              </span>
            </Tooltip>
          )
        }

        <Popconfirm
          title="确认删除知识关联吗？"
          onConfirm={() => onDeleteDoc(doc.id)}
          okText="确认"
          cancelText="取消"
          placement="topRight"
          arrowPointAtCenter
        >
          <Icon type="delete" className="c7n-docItem-delete" />
        </Popconfirm>
      </div>
    );
  }
}

export default withRouter(DocItem);
