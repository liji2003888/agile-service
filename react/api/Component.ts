import { stores, axios } from '@choerodon/boot';
import { getProjectId } from '@/utils/common';

const { AppState } = stores;
interface IComponent {
  defaultAssigneeRole: string,
  description: string
  managerId: number,
  projectId:number,
  name: string,
}
class ComponentApi {
  get prefix() {
    return `/agile/v1/projects/${getProjectId()}`;
  }

  loadComponents(pagination: any, filters: object, componentId: number) {
    const { current, pageSize } = pagination;
    const page = current;
    const size = pageSize;
    if (componentId) {
      return axios.post(`${this.prefix}/component/query_all`, filters, {
        params: {
          componentId,
          page,
          size,
          no_issue_test: true,
        },
      });
    }
    return axios.post(`${this.prefix}/component/query_all?no_issue_test=true&page=${page}&size=${size}`, filters);
  }

  loadAllComponents(filter?: string, projectId?: number) {
    return axios.post(
      `/agile/v1/projects/${projectId || getProjectId()}/component/query_all?size=${999}&page=${1}`, {
        advancedSearchArgs: {},
        searchArgs: {},
        content: filter,
      },
    );
  }

  /**
   * 加载全部模块
   */
  loadAll() {
    return axios.get(`${this.prefix}/component`);
  }

  /**
   * 创建模块
   * @param obj 
   */
  create(obj: IComponent) {
    const projectId:number = AppState.currentMenuType.id;
    const component = {
      ...obj,
      projectId,
    };
    return axios.post(
      `${this.prefix}/component`,
      component,
    );
  }

  /**
   * 更新模块
   * @param componentId 
   * @param obj 
   */
  update(componentId: number, obj: object) {
    const projectId = AppState.currentMenuType.id;
    const component = {
      projectId,
      ...obj,
    };
    return axios.put(
      `${this.prefix}/component/${componentId}`,
      component,
    );
  }

  /**
   * 根据模块id加载模块
   * @param componentId 
   */
  load(componentId: number) {
    return axios.get(`${this.prefix}/component/${componentId}`);
  }

  /**
   * 删除模块
   * @param componentId 
   * @param relateComponentId 
   */
  delete(componentId: number, relateComponentId: number) {
    if (relateComponentId === 0) {
      return axios.delete(`${this.prefix}/component/${componentId}`);
    }
    return axios({
      method: 'delete',
      url: `${this.prefix}/component/${componentId}`,
      params: {
        relateComponentId,
      },
    });
  }

  /**
   * 检查模块名称是否重复
   * @param componentName 
   */
  checkName(componentName:string) {
    return axios({
      method: 'get',
      url: `${this.prefix}/component/check_name`,
      params: {
        componentName,
      },
    });
  }
}

const componentApi = new ComponentApi();

export { componentApi };
