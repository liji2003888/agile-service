import React, { useContext } from 'react';
import { observer } from 'mobx-react-lite';
import { Dropdown, Menu, Icon } from 'choerodon-ui';
import { find } from 'lodash';
import { useIssueTypes } from '@/hooks';
import { issueApi } from '@/api';
import TypeTag from '../../TypeTag';
import IsInProgramStore from '../../../stores/common/program/IsInProgramStore';
import EditIssueContext from '../stores';
import './IssueComponent.less';


const IssueType = observer(({
  reloadIssue, onUpdate,
}) => {
  const { store, disabled } = useContext(EditIssueContext);
  let [issueTypeData] = useIssueTypes();
  const handleChangeType = (type) => {
    const issue = store.getIssue;
    const {
      issueId, objectVersionNumber, summary, featureVO = {}, issueTypeVO = {},
    } = issue;
    const { featureType, value } = type.item.props;
    const { typeCode } = issueTypeVO;
    if (typeCode === 'feature') {
      const { id, objectVersionNumber: featureObjNum } = featureVO;
      const issueUpdateVO = {
        issueId,
        objectVersionNumber,
        featureVO: {
          id,
          issueId,
          objectVersionNumber: featureObjNum,
          featureType: type.item.props.value,
        },
      };
      issueApi.update(issueUpdateVO)
        .then(() => {
          if (reloadIssue) {
            reloadIssue(issueId);
          }
          if (onUpdate) {
            onUpdate();
          }
        });
    } else {
      const issueUpdateTypeVO = {
        epicName: type.key === 'issue_epic' ? summary : undefined,
        issueId,
        objectVersionNumber,
        typeCode: type.key,
        issueTypeId: value,
        featureType,
      };
      issueApi.updateType(issueUpdateTypeVO)
        .then(() => {
          if (reloadIssue) {
            reloadIssue(issueId);
          }
          if (onUpdate) {
            onUpdate();
          }
        });
    }
  };

 
  const issue = store.getIssue;
  const { issueTypeVO = {}, featureVO = {}, subIssueVOList = [] } = issue;
  const { typeCode } = issueTypeVO;
  const { stateMachineId } = find(issueTypeData, { typeCode }) || {};
  const { featureType } = featureVO || {};
  let currentIssueType = issueTypeVO;
  if (typeCode === 'feature') {
    issueTypeData = [
      {
        ...issueTypeVO,
        colour: '#3D5AFE',
        featureType: 'business',
        name: '特性',
        id: 'business',
      }, {
        ...issueTypeVO,
        colour: '#FFCA28',
        featureType: 'enabler',
        name: '使能',
        id: 'enabler',
      },
    ];
    currentIssueType = featureType === 'business' ? issueTypeData[0] : issueTypeData[1];
  } else {
    issueTypeData = issueTypeData.filter(item => item.stateMachineId === stateMachineId).filter(item => ![typeCode, 'feature', 'sub_task'].includes(item.typeCode));
    if (IsInProgramStore.isInProgram) {
      issueTypeData = issueTypeData.filter(item => item.typeCode !== 'issue_epic');
    }
  }
  if (subIssueVOList.length > 0) {
    issueTypeData = issueTypeData.filter(item => ['task', 'story'].includes(item.typeCode));
  }
  const typeList = (
    <Menu
      style={{
        background: '#fff',
        boxShadow: '0 5px 5px -3px rgba(0, 0, 0, 0.20), 0 8px 10px 1px rgba(0, 0, 0, 0.14), 0 3px 14px 2px rgba(0, 0, 0, 0.12)',
        borderRadius: '2px',
      }}
      className="issue-sidebar-types"
      onClick={handleChangeType}
    >
      {
        issueTypeData.map(t => (
          <Menu.Item key={t.typeCode} value={t.id} featureType={t.featureType}>
            <TypeTag
              style={{ margin: 0 }}
              data={t}
              showName
              featureType={t.featureType}
            />
          </Menu.Item>
        ))
      }
    </Menu>
  );

  return (
    <div>
      {disabled ? (
        <div
          style={{
            height: 50,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <TypeTag
            data={currentIssueType}
            featureType={featureType}
          />
        </div>
      ) : (
        <Dropdown overlay={typeList} trigger={['click']} disabled={typeCode === 'sub_task' || disabled}>
          <div
            className="issue-nav-narrow"
          >
            <TypeTag
              data={currentIssueType}
              featureType={featureVO && featureVO.featureType}
            />
            {typeCode !== 'sub_task' && (
            <Icon
              type="arrow_drop_down"
              style={{ fontSize: 16 }}
            />
            )}
          </div>
        </Dropdown>
      )}
    </div>
  );
});
export default IssueType;
