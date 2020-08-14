import React, { Component } from 'react';
import { Input } from 'choerodon-ui';
import { getProjectId } from '@/utils/common';
import { issueApi, fieldApi } from '@/api';
import Card from '../Card';
import './CreateStory.less';
import StoryMapStore from '../../../../../../stores/project/StoryMap/StoryMapStore';
import clickOutSide from '../../../../../../components/CommonComponent/ClickOutSide';

class CreateStory extends Component {
  // 防止重复创建
  canAdd = true;

  state = {
    adding: false,
    value: '',
  }

  handleClickOutside = (e) => {
    const { adding } = this.state;
    if (!adding) {
      return;
    }
    this.handleCreateIssue();
  };

  handleCreateIssue = () => {
    if (!this.canAdd) {
      return;
    }
    this.canAdd = false;
    const { value } = this.state;
    if (value && value.trim()) {
      const { swimLine } = StoryMapStore;
      const {
        onCreate, epic, feature, version,
      } = this.props;
      const storyType = StoryMapStore.getIssueTypeByCode('story');
      const defaultPriority = StoryMapStore.getDefaultPriority;
      const req = {
        epicId: epic.issueId,
        featureId: feature.issueId === 'none' ? 0 : feature.issueId,
        projectId: getProjectId(),
        summary: value,
        typeCode: 'story',
        issueTypeId: storyType.id,
        priorityCode: `priority-${defaultPriority.id}`,
        priorityId: defaultPriority.id,
        ...swimLine === 'version' && version.versionId !== 'none' ? {
          versionIssueRelVOList: [{
            ...version,
            relationType: 'fix',
          }],
        } : {},
      };
      issueApi.create(req).then((res) => {
        const dto = {
          schemeCode: 'agile_issue',
          context: res.typeCode,
          pageCode: 'agile_issue_create',
        };
        this.setState({
          adding: false,
          value: '',
        });
        const { versionIssueRelVOList } = res;
        onCreate({ ...res, storyMapVersionDTOList: versionIssueRelVOList });
        fieldApi.quickCreateDefault(res.issueId, dto);
      }).finally(() => {
        this.canAdd = true;
      });
    } else {
      this.canAdd = true;
      this.setState({
        adding: false,
        value: '',
      });
    }
  }

  handleAddStoryClick = () => {
    this.setState({
      adding: true,
    });
  }

  handleChange = (e) => {
    this.setState({
      value: e.target.value,
    });
  }

  handleSourceClick = () => {
    StoryMapStore.setSideIssueListVisible(true);
  }

  render() {
    const { adding, value } = this.state;
    return (
      <Card
        style={{
          boxShadow: adding ? '0 0 4px -2px rgba(0,0,0,0.50), 0 2px 4px 0 rgba(0,0,0,0.13)' : '',
          borderRadius: 2,
          padding: 7,
          display: 'flex',
          justifyContent: 'center',
        }}
        className="c7nagile-StoryMap-CreateStory"
      >
        {
          adding
            ? <Input autoFocus onPressEnter={this.handleCreateIssue} placeholder="在此创建新内容" value={value} onChange={this.handleChange} maxLength={44} />
            : (
              <div className="c7nagile-StoryMap-CreateStory-btn">
                <span role="none" className="primary" style={{ cursor: 'pointer' }} onClick={this.handleAddStoryClick}>新建问题</span>
                {' '}
                或
                {' '}
                <span role="none" className="primary" style={{ cursor: 'pointer' }} onClick={this.handleSourceClick}>从需求池引入</span>
              </div>
            )
        }
      </Card>
    );
  }
}

CreateStory.propTypes = {

};

export default clickOutSide(CreateStory);
