import React from 'react';
import { Icon } from 'choerodon-ui';
import { Button } from 'choerodon-ui/pro';
import { getFileSuffix } from '@/utils/common';
import './index.less';
import PdfViewer from './PdfViewer';

const officeSuffix = ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'];
const prefixCls = 'c7n-agile-preview';
const Preview = ({
  fileService, fileName, fileUrl, modal,
}) => {
  const renderPreviewContent = () => {
    if (officeSuffix.includes(getFileSuffix(fileUrl))) {
      return (
        <div className={`${prefixCls}-content-iframeWrap`}>
          <iframe title="附件预览" width="100%" height="100%" src={`https://view.officeapps.live.com/op/view.aspx?src=${fileService || ''}${encodeURIComponent(fileUrl)}`} />
        </div>
      );
    } else if (getFileSuffix(fileUrl) === 'pdf') {
      return (
        <PdfViewer file={`${fileService || ''}${fileUrl}`} />       
      );
    } else {
      return (
        <div className={`${prefixCls}-content-imageWrap`}>
          <img className={`${prefixCls}-content-image`} src={`${fileService || ''}${fileUrl}`} alt="图片附件" />
        </div>
      );
    }
  };
  const handleClose = () => {
    modal.close();
  };
  return (
    <div className={`${prefixCls}`}>
      <div className={`${prefixCls}-toolbar`}>
        <Button funcType="flat" className={`${prefixCls}-header-downloadWrap`}>
          <span className={`${prefixCls}-header-downloadWrap-span`}>
            <a style={{ marginRight: 6 }} href={`${fileService || ''}${fileUrl}`}>
              <Icon type="get_app" style={{ color: '#000' }} />
              <span className={`${prefixCls}-header-downloadWrap-fileName`}>{decodeURIComponent(fileName)}</span>
            </a>
          </span>
        </Button>
        <Icon type="close" style={{ fontSize: 20, marginLeft: 20, cursor: 'pointer' }} onClick={handleClose} />
      </div>
      <div className={`${prefixCls}-content`}>
        {renderPreviewContent()}
      </div>
    </div>
  );
};

export default Preview;
