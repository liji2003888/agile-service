import { observable, action, computed } from 'mobx';
import { Choerodon } from '@choerodon/boot';
import querystring from 'query-string';
import { statusApi } from '@/api';

class StateStore {
  @observable stateList = [];

  @observable isLoading = false;

  @computed get getStateList() {
    return this.stateList;
  }

  @action setStateList(data) {
    this.stateList = data;
  }

  @computed get getIsLoading() {
    return this.isLoading;
  }

  @action setIsLoading(loading) {
    this.isLoading = loading;
  }

  loadStateList = (orgId, page, size, sort = { field: 'id', order: 'desc' }, param) => {
    this.setIsLoading(true);

    return statusApi.loadList(page, size, `${sort.field},${sort.order}`, param).then((data) => {
      this.setStateList(data.list);
      if (data && data.failed) {
        Choerodon.prompt(data.message);
        return Promise.reject(data);
      } else {
        this.setIsLoading(false);
        return Promise.resolve(data);
      }
    }).catch(() => Promise.reject());
  };
}

export default StateStore;
