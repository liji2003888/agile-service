import { axios } from '@choerodon/boot';
import { AxiosRequestConfig } from 'axios';
import { getProjectId, getOrganizationId } from '@/utils/common';
import globalCache from './Cache';

interface RequestConfig extends AxiosRequestConfig {
  cache?: boolean
  noPrompt?: boolean
}
function getCacheKey(config: any) {
  const {
    method, url, data, params,
  } = config;
  return JSON.stringify({
    method, url, data, params,
  });
}
class Api {
  isConfig: boolean;

  constructor(isConfig: boolean = false) {
    this.isConfig = isConfig;
  }

  request(AxiosConfig: RequestConfig) {
    if (this.isConfig) {
      return AxiosConfig;
    }
    const { cache } = AxiosConfig;
    if (cache) {
      return new Promise((resolve) => {
        globalCache.apply({
          request: axios.bind(null, AxiosConfig),
          cacheKey: getCacheKey(AxiosConfig),
          callback: resolve,
        });
      });
    }
    return axios(AxiosConfig);
  }

  get projectId() {
    return getProjectId();
  }

  get orgId() {
    return getOrganizationId();
  }

  /**
   * 目的：达到api.project(2).getList()中getList获取的projectId为2，而不是当前projectId
   * @param Property 要覆盖的key
   * @param value 要覆盖的值
   */
  overwrite(Property: string, value: any) {
    // 以当前this为模板，创建一个新对象
    const temp = Object.create(this);
    // 不直接temp[Property] = value;的原因是，如果这个属性只有getter，会报错
    Object.defineProperty(temp, Property, {
      get() {
        return value;
      },
    });
    // 返回新对象
    return temp;
  }

  project(projectId: number) {
    return this.overwrite('projectId', projectId);
  }

  org(orgId: number) {
    return this.overwrite('orgId', orgId);
  }
}

export default Api;
