import React, { useEffect, useState } from 'react';
import { Button } from 'choerodon-ui/pro';
import { observer } from 'mobx-react-lite';
import IssueSearch, { IssueSearchStore, useIssueSearchStore } from '@/components/issue-search';
import { localPageCacheStore } from '@/stores/common/LocalPageCacheStore';
import { getSystemFields as originGetSystemFields } from '@/stores/project/issue/IssueStore';
import { transformFilter as originTransformFilter } from '@/routes/Issue/stores/utils';
import openSaveFilterModal from '@/components/SaveFilterModal';
import { Tooltip } from 'choerodon-ui';
import scrumBoardStore from '@/stores/project/scrumBoard/ScrumBoardStore';
import { IChosenFields, ILocalField } from '@/components/issue-search/store';
import SelectSprint from '@/components/select/select-sprint';
import './index.less';

interface Props {
    onRefresh: () => void
    saveStore: (store: IssueSearchStore) => void
    //   issueSearchStore: IssueSearchStore,
}
function getSystemFields() {
  const systemFields = originGetSystemFields(['quickFilterIds', 'issueIds', 'sprint', 'assigneeId', 'priorityId']);
  systemFields.unshift(...[{
    code: 'quickFilterIds',
    name: '快速筛选',
    defaultShow: true,
    noDisplay: true,
  }, {
    code: 'assigneeId',
    name: '经办人',
    defaultShow: true,
    fieldType: 'member',
  }, {
    code: 'sprint',
    name: '冲刺',
    defaultShow: true,
    fieldType: 'single',
  }, {
    code: 'priorityId',
    name: '优先级',
    defaultShow: true,
    fieldType: 'multiple',
  },
  ]);
  return systemFields;
}
function renderField(field: ILocalField, props: any) {
  if (field.code === 'sprint') {
    return (
      <SelectSprint
        key={field.code}
        flat
        placeholder={field.name}
        maxTagTextLength={10}
        dropdownMatchSelectWidth={false}
        clearButton
        optionRenderer={({ record, text }) => {
          if (record?.get('statusCode') === 'started') {
            return (
              <span>
                {text}
                <div className="c7n-agile-sprintSearchSelect-option-active">活跃</div>
              </span>
            );
          }
          return <span>{text}</span>;
        }}
        {...props}
      />
    );
  }
  return null;
}
function transformFilter(chosenFields:IChosenFields) {
  const filter = originTransformFilter(chosenFields);
  if (filter.otherArgs.sprint && filter.otherArgs.sprint !== '') {
    filter.otherArgs.sprint = [filter.otherArgs.sprint];
  }
  return filter;
}
const BoardSearch: React.FC<Props> = ({ onRefresh, saveStore }) => {
  const [visible, setVisible] = useState<boolean>(false);

  const issueSearchStore = useIssueSearchStore({
    // @ts-ignore
    getSystemFields,
    transformFilter,
    renderField,
    // @ts-ignore
    defaultChosenFields: Array.isArray(localPageCacheStore.getItem('scrumBoard.search')) ? new Map(localPageCacheStore.getItem('scrumBoard.search').map((item) => [item.code, item])) : undefined,
  });
  const handleClear = () => {
    scrumBoardStore.clearFilter();
    localPageCacheStore.remove('scrumBoard.search');
    localPageCacheStore.remove('scrumBoard.searchVO');
    onRefresh();
    // StoryMapStore.clearData();
    // StoryMapStore.getStoryMap();
  };
  useEffect(() => {
    console.log('isHasFilter.', (!scrumBoardStore.didCurrentSprintExist
        || ((!scrumBoardStore.otherIssue || scrumBoardStore.otherIssue.length === 0)
          && (!scrumBoardStore.interconnectedData
            || scrumBoardStore.interconnectedData.size === 0))), issueSearchStore.isHasFilter, issueSearchStore.getFilterValueByCode('sprint'));
    // scrumBoardStore.setIsHasFilter(issueSearchStore.isHasFilter);
  }, [issueSearchStore.isHasFilter]);
  useEffect(() => {
    saveStore(issueSearchStore);
  }, [issueSearchStore, saveStore]);
  const handleClickSaveFilter = () => {
    openSaveFilterModal({ searchVO: issueSearchStore.getCustomFieldFilters(), onOk: issueSearchStore.loadMyFilterList });
  };

  return (
    <div className="c7n-agile-scrum-board-search" style={{ display: 'flex' }}>
      <IssueSearch
        store={issueSearchStore}
        onClear={handleClear}
        onClickSaveFilter={handleClickSaveFilter}
        onChange={() => {
          const newSearch = issueSearchStore.getCustomFieldFilters();
          localPageCacheStore.setItem('scrumBoard.search', issueSearchStore.currentFilter);
          localPageCacheStore.setItem('scrumBoard.searchVO', newSearch);
          //   ScrumBoardStore
          scrumBoardStore.setSearchVO(newSearch);
          onRefresh();
          //   StoryMapStore.setSearchVO(issueSearchStore.getCustomFieldFilters());
          //   StoryMapStore.clearData();
          //   StoryMapStore.getStoryMap();
        }}
      />

    </div>

  );
};

export default observer(BoardSearch);