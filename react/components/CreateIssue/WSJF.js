import React from 'react';
import { Form } from 'choerodon-ui';
import SelectNumber from '@/components/SelectNumber';
import { Tooltip, Icon } from 'choerodon-ui';

const FormItem = Form.Item;
function WSJF({ getFieldDecorator }) {
  return (
    <div>
      <h3 style={{ marginLeft: 10 }}>
        WSJF
        <Tooltip title="加权最短作业优先（WSJF）适用于对作业（例如功能，功能和史诗）进行排序以产生最大的经济效益的优先级模型。在SAFe中，WSJF估算为延迟成本（CoD）除以工作规模">
          <Icon type="help" style={{ color: 'rgba(0, 0, 0, 0.65)', marginLeft: 2, marginTop: -5 }} />
        </Tooltip>
      </h3>      
      <FormItem key="userBusinessValue">
        {getFieldDecorator('userBusinessValue', {
        })(
          <SelectNumber label="用户/业务价值" selectNumbers={['1', '2', '3', '5', '8', '13', '20']} />,
        )}
      </FormItem>
      <FormItem key="timeCriticality">
        {getFieldDecorator('timeCriticality', {
        })(
          <SelectNumber label="时间紧迫性" selectNumbers={['1', '2', '3', '5', '8', '13', '20']} />,
        )}
      </FormItem>
      <FormItem key="rrOeValue">
        {getFieldDecorator('rrOeValue', {
        })(
          <SelectNumber label="降低风险|促成机会" selectNumbers={['1', '2', '3', '5', '8', '13', '20']} />,
        )}
      </FormItem>
      <FormItem key="jobSize">
        {getFieldDecorator('jobSize', {
          initialValue: '1',
        })(
          <SelectNumber label="工作规模" selectNumbers={['1', '2', '3', '5', '8', '13', '20']} />,
        )}
      </FormItem>
    </div>
  );
}

export default WSJF;
