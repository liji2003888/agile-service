import React, {
  useCallback, useState, useRef,
} from 'react';

import { Animate } from 'choerodon-ui';
import { stores } from '@choerodon/boot';
import Container, { registerPath } from './Container';
import PreviewIssueFile from './PreviewIssueFile';
import DetailContainerContext, { IPreview, IRoute, IRouteWithKey } from './context';
// 默认展示children，push之后再匹配
const { HeaderStore } = stores;
export { registerPath };
export interface DetailEvents {
  [type: string]: () => void
}
export const useDetail = (): [DetailContainerProps] => {
  const [routes, setRoutes] = useState<IRouteWithKey[]>([]);
  const [visible, setVisible] = useState(false);
  const [filePreview, setFilePreview] = useState<IPreview>();
  const [hidden, setHidden] = useState(false);
  const eventsMap = useRef<Map<string, DetailEvents>>(new Map());
  const updateEventsMap = useCallback((path: string, events?: DetailEvents) => {
    if (events) {
      eventsMap.current.set(path, events);
    }
  }, []);
  const match = routes[routes.length - 1];
  const push = useCallback((nextRoute: IRoute) => {
    const routeWithKey = { ...nextRoute, key: Math.random() };
    setRoutes((r) => ([...r, routeWithKey]));
    updateEventsMap(routeWithKey.path, routeWithKey.events);
    setVisible(true);
  }, [updateEventsMap]);
  const open = useCallback((route: IRoute) => {
    const routeWithKey = { ...route, key: Math.random() };
    setRoutes([routeWithKey]);
    updateEventsMap(routeWithKey.path, routeWithKey.events);
    setVisible(true);
  }, [updateEventsMap]);
  const pop = useCallback(() => {
    setRoutes((r) => {
      const clone = [...r];
      clone.pop();
      return clone;
    });
  }, []);
  const close = useCallback(() => {
    if (filePreview) {
      setHidden(true);
    } else {
      setVisible(false);
    }
  }, [filePreview]);
  const clear = useCallback(() => {
    eventsMap.current.forEach((events) => {
      if (events.close) {
        events.close();
      }
    });
    setRoutes([]);
  }, []);
  return [{
    visible,
    routes,
    match,
    open,
    push,
    pop,
    close,
    clear,
    eventsMap: eventsMap.current,
    filePreview,
    setFilePreview,
    hidden,
    setHidden,
    // setVisible,
  }];
};
interface DetailContainerProps {
  visible: boolean
  routes: IRouteWithKey[]
  match: IRouteWithKey
  open: (route: IRoute) => void
  push: (nextRoute: IRoute) => void
  pop: () => void
  close: () => void
  clear: () => void
  eventsMap: Map<string, DetailEvents>
  fullPage?: boolean
  filePreview?: IPreview
  setFilePreview: (filePreview?: IPreview) => void
  hidden: boolean
  setHidden: (hidden: boolean) => void
}
const DetailContainer: React.FC<DetailContainerProps> = ({ children, visible, ...props }) => {
  const resizeRef = useRef();

  const element = visible ? (
    <Container>{children}</Container>
  ) : null;
  return (
    <DetailContainerContext.Provider value={{
      topAnnouncementHeight: HeaderStore.announcementClosed ? 0 : 50,
      outside: false,
      resizeRef,
      ...props,
    }}
    >
      <>
        <PreviewIssueFile />
        {props.fullPage ? element : (
          <Animate
            component="div"
            transitionAppear
            transitionName="slide-right"
            onLeave={props.clear}
          >
            {element}
          </Animate>
        )}
      </>
    </DetailContainerContext.Provider>
  );
};

export default DetailContainer;
