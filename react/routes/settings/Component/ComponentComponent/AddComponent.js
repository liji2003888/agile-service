/* eslint-disable */
import React, { Component, Fragment, useState, useImperativeHandle, forwardRef } from 'react';
import {
  Form, Input, Select, message, Button,
} from 'choerodon-ui';
import { Modal } from 'choerodon-ui/pro';
import { Content, stores, Choerodon } from '@choerodon/boot';
import { componentApi } from '@/api';
import _ from 'lodash';
import UserHead from '../../../../components/UserHead';
import './component.less';
import { userApi } from '@/api';

const { TextArea } = Input;
const { Option } = Select;
const { AppState } = stores;
const FormItem = Form.Item;
let sign = false;

function AddComponent(props) {
  const { modal } = props;

  const [originUsers, setOriginUsers] = useState([]);
  const [selectLoading, setSelectLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [canLoadMore, setCanLoadMore] = useState(true);
  const [input, setInput] = useState('');

  const { getFieldDecorator, getFieldsValue } = props.form;
  const handleSubmit = () => {
    props.form.validateFields((err, values) => {
      if (!err) {
        const {
          defaultAssigneeRole, description, managerId, name,
        } = values;
        const component = {
          defaultAssigneeRole,
          description,
          managerId: managerId ? JSON.parse(managerId).id || 0 : 0,
          name: name.trim(),
        };
        componentApi.create(component)
          .then((res) => {
            props.modal.close();
            props.onOk();
          })
          .catch((error) => {
            Choerodon.prompt('创建模块失败');
          });
      }
    });
    return false;
  };
  modal.handleOk(handleSubmit)
  const onFilterChange = (input) => {
    if (!sign) {
      setSelectLoading(true);
      userApi.getAllInProject(input, page).then((res) => {
        setInput(input);
        setOriginUsers(res.list.filter(u => u.enabled));
        setCanLoadMore(res.hasNextPage);
        setSelectLoading(false);
      });
      sign = true;
    } else {
      debounceFilterIssues(input, undefined, page);
    }
  };

  const debounceFilterIssues = _.debounce((input) => {
    setSelectLoading(true);
    setPage(1);
    userApi.getAllInProject(input, page).then((res) => {
      setInput(input);
      setPage(1);
      setOriginUsers(res.list.filter(u => u.enabled));
      setSelectLoading(false);
      setCanLoadMore(res.hasNextPage);
    });
  }, 500);

  const getFirst = (str) => {
    if (!str) {
      return '';
    }
    const re = /[\u4E00-\u9FA5]/g;
    for (let i = 0, len = str.length; i < len; i += 1) {
      if (re.test(str[i])) {
        return str[i];
      }
    }
    return str[0];
  };



  const checkComponentNameRepeat = (rule, value, callback) => {
    if (value && value.trim()) {
      componentApi.checkName(value.trim()).then((res) => {
          if (res) {
            callback('模块名称重复');
          } else {
            callback();
          }
        });
    } else {
      callback();
    }
  };

  const loadMoreUsers = (e) => {
    e.preventDefault();
    setSelectLoading(true);
    userApi.getAllInProject(input, page + 1).then((res) => {
      setOriginUsers([...originUsers, ...res.list.filter(u => u.enabled)]);
      setSelectLoading(false);
      setCanLoadMore(res.hasNextPage);
      setPage(page + 1);
    });
  };

  return (
    <Form className="c7n-component-component">
      <FormItem style={{ marginBottom: 20 }}>
        {getFieldDecorator('name', {
          rules: [{
            required: true,
            message: '模块名称必填',
            whitespace: true,
          }, {
            validator: checkComponentNameRepeat,
          }],
        })(
          <Input label="模块名称" maxLength={10} />,
        )}
      </FormItem>
      <FormItem style={{ marginBottom: 20 }}>
        {getFieldDecorator('description', {})(
          <Input label="模块描述" maxLength={30} />,
        )}
      </FormItem>
      <FormItem style={{ marginBottom: 20 }}>
        {getFieldDecorator('defaultAssigneeRole', {
          rules: [{
            required: true,
            message: '默认经办人必填',
          }],
        })(
          <Select label="默认经办人">
            {['模块负责人', '无'].map(defaultAssigneeRole => (
              <Option key={defaultAssigneeRole} value={defaultAssigneeRole}>
                {defaultAssigneeRole}
              </Option>
            ))}
          </Select>,
        )}
      </FormItem>
      {getFieldsValue(['defaultAssigneeRole']).defaultAssigneeRole && getFieldsValue(['defaultAssigneeRole']).defaultAssigneeRole === '模块负责人' && (
        <FormItem style={{ marginBottom: 20 }}>
          {getFieldDecorator('managerId', {})(
            <Select
              label="负责人"
              loading={selectLoading}
              allowClear
              filter
              onFilterChange={onFilterChange}
              dropdownClassName="hidden-text hidden-label"
            >
              {
                originUsers.map(user => (
                  <Option key={JSON.stringify(user)} value={JSON.stringify(user)}>
                    <div style={{ display: 'inline-flex', alignItems: 'center', padding: '2px' }}>
                      <UserHead
                        user={user}
                      />
                    </div>
                  </Option>
                ))
              }
              {
                canLoadMore && <Option key='loadMore' disabled className='loadMore-option'>
                  <Button type="primary" onClick={loadMoreUsers} className="option-btn">更多</Button>
                </Option>
              }
            </Select> ,
          )}
        </FormItem>
      )}
    </Form>
  );
}

export default Form.create()(AddComponent);

