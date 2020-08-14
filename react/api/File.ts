import { axios } from '@choerodon/boot';
import { getProjectId } from '@/utils/common';

class FileApi {
  get prefix() {
    return `/agile/v1/projects/${getProjectId()}`;
  }


  /**
    * 上传issue的附件
    * @param {*} data
    * @param {*} config
    */
  uploadFile(data: FormData, issueId: number, projectId?: number) {
    const headers = { 'content-type': 'multipart/form-data' };
    return axios({
      headers,
      method: 'post',
      url: `/agile/v1/projects/${projectId || getProjectId()}/issue_attachment`,
      data,
      params: {
        // projectId,
        issueId,
      },
    });
  }
  
  /**
    * 上传图片
    * @param {any} data
    */
  uploadImage(data:FormData) {
    const headers = { 'content-type': 'multipart/form-data' };
    return axios({
      headers,
      method: 'post',
      url: `${this.prefix}/issue_attachment/upload_for_address`,
      data,
    });
  }

  /**
    * 删除文件
    * @param {number} resourceId 文件资源id
    */
  deleteFile(fileId:number) {
    return axios.delete(`${this.prefix}/issue_attachment/${fileId}`);
  }
}

const fileApi = new FileApi();

export { fileApi };
