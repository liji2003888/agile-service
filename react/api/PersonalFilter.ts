import { axios, stores } from '@choerodon/boot';
import { getProjectId } from '@/utils/common';

interface IPersonalFilter {
    filterJson: string, // 搜索条件json字符串
    name: string,
}
interface UPersonalFilter{
    name: string,
    objectVersionNumber: number,
}
const { AppState } = stores;
class PersonalFilterApi {
  get prefix() {
    return `/agile/v1/projects/${getProjectId()}`;
  }

  /**
  * 查询用户的全部筛选
  * @param userId 默认查询当前用户
  */
  loadAll(userId: number = AppState.userInfo.id) {
    // const { userInfo: { id: userId } } = AppState;
    return axios({
      method: 'get',
      url: `${this.prefix}/personal_filter/query_all/${userId}`,
    });
  }

  /**
            * 创建我的筛选
            * @param data 
            */
  create(data: IPersonalFilter) {
    return axios.post(`${this.prefix}/personal_filter`, data);
  }

  /**
           * 更新我的筛选
           * @param filterId 
           * @param updateData 
           */
  update(filterId: number, updateData: UPersonalFilter) {
    return axios.put(`${this.prefix}/personal_filter/${filterId}`, updateData);
  }

  /**
    * 删除我的筛选
    * @param filterId 
    */
  delete(filterId: number) {
    return axios.delete(`${this.prefix}/personal_filter/${filterId}`);
  }

  /** 
    * 检查名字是否重复
    * @param name 
    */
  checkName(name: string) {
    const userId = AppState.userInfo.id;
    return axios({
      method: 'get',
      url: `${this.prefix}/personal_filter/check_name`,
      params: {
        userId,
        name,
      },
    });
  }
}

const personalFilterApi = new PersonalFilterApi();
export { personalFilterApi };
