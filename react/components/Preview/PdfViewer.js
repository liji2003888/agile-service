import pdfjs from 'pdfjs-dist/build/pdf';
import { PDFPageView } from 'pdfjs-dist/web/pdf_viewer';
import 'pdfjs-dist/web/pdf_viewer.css';
import React, {
  useState, useEffect, useRef, useMemo,
} from 'react';
import { useUnmountedRef } from 'ahooks';
import Loading from '../Loading';
import './PdfViewer.less';

export const usePdf = ({
  container,
  file,
  scale = 1.0,
  rotate = 0,
  page = 1,
  cMapUrl,
  cMapPacked,
  workerSrc = '//cdnjs.cloudflare.com/ajax/libs/pdf.js/2.2.228/pdf.worker.js',
  withCredentials = false,
}) => {
  const [pdf, setPdf] = useState();
  const unmountRef = useUnmountedRef();
  const rafRef = useRef(null);
  useEffect(() => {
    pdfjs.GlobalWorkerOptions.workerSrc = workerSrc;
  }, []);

  useEffect(() => {
    const config = { url: file, withCredentials };
    if (cMapUrl) {
      config.cMapUrl = cMapUrl;
      config.cMapPacked = cMapPacked;
    }
    pdfjs.getDocument(config).promise.then(setPdf);
  }, [file, withCredentials]);
  // 渲染一页
  const drawPDF = (pageContent) => {
    if (unmountRef.current) {
      return null;
    }
    const pdfPageView = new PDFPageView({
      container: container.current,
      id: page,
      scale,
      defaultViewport: pageContent.getViewport({ scale }),
    });
    pdfPageView.setPdfPage(pageContent);
    return pdfPageView.draw();
  };

  const loading = useMemo(() => !pdf, [pdf]);
  // eslint-disable-next-line no-underscore-dangle
  const numPages = useMemo(() => (pdf ? pdf._pdfInfo.numPages : null), [pdf]);

  useEffect(() => {
    if (pdf) {
      // eslint-disable-next-line no-param-reassign
      container.current.innerHTML = '';
      // eslint-disable-next-line no-inner-declarations
      function renderPdf(curTotal, curIndex) {
        if (curTotal <= 0) {
          return false;
        }
        rafRef.current = window.requestAnimationFrame(() => {
          pdf.getPage(curIndex).then((p) => drawPDF(p));
          renderPdf(curTotal - 1, curIndex + 1);
        });
        return null;
      }
      renderPdf(numPages, 1);
    }
    return () => {
      window.cancelAnimationFrame(rafRef.current);
    };
  }, [pdf, scale, rotate]);

  return [loading, numPages];
};

const Pdf = ({
  file,
  onDocumentComplete,
  scale,
  rotate,
  cMapUrl,
  cMapPacked,
  workerSrc,
  withCredentials,
}) => {
  const container = useRef(null);
  const [page, setPage] = useState(1);
  const [loading, numPages] = usePdf({
    container,
    file,
    page,
    scale,
    rotate,
    cMapUrl,
    cMapPacked,
    workerSrc,
    withCredentials,
  });

  useEffect(() => {
    onDocumentComplete(numPages);
  }, [numPages]);

  return (
    <div className="c7n-pdf-viewer">
      <div className="c7n-pdf-viewer-content" ref={container} />
      {loading && <div><Loading /></div>}
    </div>
  );
};

Pdf.defaultProps = {
  onDocumentComplete: () => { },
};

export default Pdf;
