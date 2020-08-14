import { omit } from 'lodash';
import { axios, stores } from '@choerodon/boot';
import { getProjectId, getOrganizationId } from '@/utils/common';

const { AppState } = stores;

interface ICardStatus {
  completed: boolean,
  id: number,
  objectVersionNumber: number,
  projectId: number,
  statusId: number,
}
interface UMoveStatus {
  columnId: number,
  originColumnId: number,
  position: number,
  statusObjectVersionNumber: number,
}
interface UBoard {
  boardId: number,
  columnConstraint: string, // 列约束
  objectVersionNumber: number,
  projectId: number,
}
interface BoardSearchVO {
  onlyMe?: boolean,
  onlyStory?: boolean,
  assigneeId?: number,
  quickFilterIds?: Array<number>,
  assigneeFilterIds?: Array<number>,
  sprintId?: number,
  personalFilterIds?: string[]
  priorityIds?:string[]
}
/**
 * 迭代看板
 * @author dzc
 */
class BoardApi {
  get prefix() {
    return `/agile/v1/projects/${getProjectId()}`;
  }

  load(boardId: number, searchVO: BoardSearchVO) {
    return axios({
      method: 'post',
      url: `${this.prefix}/board/${boardId}/all_data/${getOrganizationId()}`,
      data: {
        ...omit(searchVO, 'onlyMe'),
        assigneeId: searchVO?.onlyMe ? AppState.getUserId : '',
      },
    });
  }

  /**
       * 加载看板列表
       */
  loadAll() {
    return axios.get(`${this.prefix}/board`);
  }

  /**
   * 加载看板中未对应的状态
   * @param boardId
   */
  loadNoColumnStatus(boardId: number, applyType: string = 'agile') {
    return axios({
      method: 'get',
      url: `${this.prefix}/issue_status/list_by_options`,
      params: {
        boardId,
        applyType,
      },
    });
  }

  /**
   * 创建一个看板
   * @param boardName
   */
  create(boardName: string) {
    return axios({
      method: 'post',
      url: `${this.prefix}/board`,
      params: {
        boardName,
      },
    });
  }

  /**
    * 更新看板
    * @param boardId
    * @param data
    */
  update(boardId: number, data: UBoard) {
    return axios({
      method: 'put',
      url: `${this.prefix}/board/${boardId}`,
      data,
    });
  }

  /**
    * 配置看板中状态卡片的更新 （目前用于设置是否完成）
    * @param id
    * @param data
    */
  updateStatus(id: number, data: ICardStatus) {
    return axios.put(`${this.prefix}/issue_status/${id}`, data);
  }

  /**
      * 更新用户泳道设置
      * @param boardId
      * @param swimlaneBasedCode
      */
  updateUserSetting(boardId: number, swimlaneBasedCode: string) {
    return axios({
      method: 'post',
      url: `${this.prefix}/board/user_setting/${boardId}`,
      params: {
        swimlaneBasedCode,
      },
    });
  }

  /**
         * 删除迭代看板
         * @param boardId
         */
  delete(boardId: number) {
    return axios.delete(`${this.prefix}/board/${boardId}`);
  }

  /** 删除迭代看板中未对应的状态
   *
   * @param statusId
   * @param applyType
   */
  deleteStatus(statusId: number, applyType: string = 'agile') {
    return axios({
      method: 'delete',
      url: `${this.prefix}/issue_status/${statusId}`,
      params: {
        applyType,
      },
    });
  }

  /**
   * 移动迭代看板issue
   * @param issueId
   * @param transformId
   * @param data
   */
  moveIssue(issueId: number, transformId: number, data: any) {
    return axios({
      method: 'post',
      url: `${this.prefix}/board/issue/${issueId}/move`,
      params: {
        transformId,
      },
      data,
    });
  }

  /**
   * 移动状态到未对应列
   * @param statusId
   * @param data
   */
  moveStatusToUnset(statusId: number, data: UMoveStatus) {
    return axios.post(`${this.prefix}/issue_status/${statusId}/move_to_uncorrespond`, data);
  }

  /**
   * 移动状态到列中
   * @param code
   * @param data
   */
  moveStatusToColumn(statusId: number, data: UMoveStatus) {
    return axios.post(`${this.prefix}/issue_status/${statusId}/move_to_column`, data);
  }

  /**
    * 检查看板名称是否重复
    * @param boardName
    */
  checkName(boardName: string) {
    return axios({
      method: 'get',
      url: `${this.prefix}/board/check_name`,
      params: {
        boardName,
      },
    });
  }
}

const boardApi = new BoardApi();
export { boardApi };
