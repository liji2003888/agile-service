import React from 'react';
import PropTypes from 'prop-types';
import { Icon, Tooltip } from 'choerodon-ui';
import { Modal } from 'choerodon-ui/pro';
import { getFileSuffix } from '@/utils/common';
import Preview from '@/components/Preview';
import './SingleFileUpload.less';


const previewSuffix = ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'pdf', 'jpg', 'jpeg', 'gif', 'png'];
const modalKey = Modal.key();
function SingleFileUplaod(props) {
  const {
    url, fileService, fileName, hasDeletePermission, onDeleteFile,
  } = props;

  const handlePreviewClick = (service, name, fileUrl) => {
    Modal.open({
      key: modalKey,
      title: '预览',
      // style: {
      //   width: '80%',
      // },
      footer: (okBtn, cancelBtn) => null,
      className: 'c7n-agile-preview-Modal',
      cancelText: '关闭',
      fullScreen: true,
      children: <Preview service={service} fileName={name} fileUrl={fileUrl} />,
    });
  };

  return (
    <div className="c7n-agile-singleFileUpload">
      <span className="c7n-agile-singleFileUpload-icon">
        {previewSuffix.includes(getFileSuffix(url)) && (
          <Tooltip title="预览">
            <Icon
              type="zoom_in"
              style={{ cursor: 'pointer' }}
              onClick={handlePreviewClick.bind(this, fileService, fileName, url)}
            />
          </Tooltip>
        )}
      </span>
      <a className="c7n-agile-singleFileUpload-download" href={fileService ? `${fileService}${url}` : `${url}`}>
        <span className="c7n-agile-singleFileUpload-icon">
          <Tooltip title="下载">
            <Icon type="get_app" style={{ color: '#000' }} />
          </Tooltip>
        </span>
        <span className="c7n-agile-singleFileUpload-fileName">{fileName}</span>
      </a>
      {(hasDeletePermission && onDeleteFile) && (
        <Tooltip title="删除">
          <Icon
            type="close"
            onClick={() => { onDeleteFile(); }}
          />
        </Tooltip>
      )}
    </div>
  );
}

SingleFileUplaod.propTypes = {
  url: PropTypes.string.isRequired,
  fileService: PropTypes.string,
  fileName: PropTypes.string.isRequired,
  hasDeletePermission: PropTypes.bool,
  onDeleteFile: PropTypes.func,
};

SingleFileUplaod.defaultProps = {
  hasDeletePermission: false,
};

export default SingleFileUplaod;
