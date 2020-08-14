import { axios } from '@choerodon/boot';
import { getProjectId } from '@/utils/common';

class PiApi {
  get prefix() {
    return `/agile/v1/projects/${getProjectId()}`;
  }

  /**
   * 根据状态获取PI
   * @param statusList 
   */
  async getPiListByStatus(statusList = ['todo', 'doing', 'done']) {
    const res = await axios({
      method: 'post',
      url: `${this.prefix}/pi/query_pi_by_status`,
      data: statusList,
    });
    return res.map((pi: { code: string, name: string}) => ({ ...pi, piName: `${pi.code}-${pi.name}` }));
  }

  /**
   * 获取没有结束的PI
   */
  getUnfinished() {
    return axios.get(`${this.prefix}/pi/unfinished`);
  }

  /**
   * 在子项目获取当前PI
   * @param programId 
   * @param artId 
   */
  getCurrent(programId: number, artId: number) {
    return axios({
      url: `${this.prefix}/pi/query_doing_pi`,
      method: 'get',
      params: {
        program_id: programId,
        art_id: artId,
      },
    });
  }

  /**
   * 子项目下,根据活跃的ART和PI状态查询PI
   * @param data  状态列表，[todo、doing、done]
   * @param programId 
   */
  getPiByPiStatus(data:Array<string>, programId:number) {
    return axios({
      method: 'post',
      url: `${this.prefix}/project_invoke_program/pi/query_pi_by_status`,
      params: {
        programId,
      },
      data,
    });
  }

  /**
   * 查询feature 关联过的特性记录(PI历程)
   * @param issueId 
   */
  getFeatureLog(issueId:number) {
    return axios.get(`${this.prefix}/pi/${issueId}/list_feature_pi_log`);
  }

  /**
   * 批量将feature加入到pi中
   * @param issueIds 
   * @param sourceId 
   * @param destinationId pi Id
   * @param before 
   * @param outsetIssueId 
   * @param rankIndex 
   */
  addFeatures(issueIds:Array<number>, sourceId:number = 0, destinationId:number = 0,
    before:boolean = false, outsetIssueId:number = 0, rankIndex:number = 0) {
    return axios.post(`${this.prefix}/pi/to_pi/${destinationId}`, {
      before,
      issueIds,
      outsetIssueId,
      rankIndex,
      currentPiId: sourceId,
    });
  }
}

const piApi = new PiApi();
export { piApi };
