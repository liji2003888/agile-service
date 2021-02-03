import React, {
  useState, useEffect, useCallback, memo, ReactElement, useMemo, useRef,
} from 'react';
import {
  Modal, TextField, Form, Button, DataSet,
} from 'choerodon-ui/pro';
import { Choerodon } from '@choerodon/boot';
import TableColumnCheckBoxes, { useTableColumnCheckBoxes } from '@/components/table-column-check-boxes';
import { IModalProps } from '@/common/types';
import { TemplateAction, templateApi } from '@/api';
import { getExportFieldCodes, getReverseExportFieldCodes } from '@/routes/Issue/components/ExportIssue/utils';
import classnames from 'classnames';
import { FieldType } from 'choerodon-ui/pro/lib/data-set/enum';
import { uniq } from 'lodash';
import styles from './EditTemplate.less';

interface FormPartProps {
  title: string | ReactElement,
  className?: string,
  children: ReactElement | ReactElement[] | null | Array<ReactElement | null>,
  btnOnClick?: (nextBtnStatusCode: 'ALL' | 'NONE') => boolean,
}

const FormPart: React.FC<FormPartProps> = memo((props) => {
  const {
    title, children, btnOnClick,
  } = props;
  const [btnStatus, setBtnStatus] = useState<'ALL' | 'NONE'>();
  function handleClick() {
    let result = true;
    const nextBtnStatus = btnStatus !== 'NONE' ? 'NONE' : 'ALL';
    if (typeof (btnOnClick) === 'function') {
      result = btnOnClick(nextBtnStatus);
    }
    result && setBtnStatus(nextBtnStatus);
  }
  return (
    <div className={classnames(styles.part, props.className)}>
      <div className={styles.part_title}>
        <span>{title}</span>
        {!!btnOnClick && (
          <Button
            className={styles.part_btn}
            onClick={handleClick}
          >
            {btnStatus !== 'NONE' ? '全选' : '全不选'}
          </Button>
        )}
      </div>
      <div className={styles.part_content}>
        {children}
      </div>
    </div>
  );
});

export interface ITemplate {
  id: string
  name: string
  objectVersionNumber: number,
  templateJson: string
}

export interface IFieldOption {
  label: string
  value: string
  disabled?: boolean,
  defaultChecked?: boolean,
  name?: string
}

interface Props {
  action: TemplateAction
  template: ITemplate
  modal?: IModalProps
  checkOptions: IFieldOption[]
  onEdit: (template: ITemplate) => void
}

const EditTemplate: React.FC<Props> = ({
  modal, template, checkOptions, action, onEdit,
}) => {
  const templateFieldsRef = useRef();

  useEffect(() => {
    Object.assign(templateFieldsRef, {
      current: getReverseExportFieldCodes(JSON.parse(template.templateJson)),
    });
  }, [template.templateJson]);

  const checkName = useCallback(async (value, name, record) => {
    if (value === template.name) {
      return true;
    }
    const res = await templateApi.checkName(value, action);
    if (!res) {
      return true;
    }
    return '模板名称重复';
  }, [action, template.name]);

  const templateDataSet = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [{
      name: 'templateName',
      label: '模板名称',
      type: 'string' as FieldType,
      maxLength: 12,
      required: true,
      validator: checkName,
    }],
  }), [checkName]);

  useEffect(() => {
    templateDataSet?.current?.set('templateName', template.name);
  }, [template.name, templateDataSet]);

  const defaultInitOptions = useCallback(({ dataSet }) => {
    dataSet.addField('required-option', { multiple: true });
    dataSet.current?.set('required-option', ['issueTypeId', 'issueNum', 'issueId']);
  }, []);

  // 选择字段框配置 数据
  const [checkBoxDataProps, checkBoxComponentProps] = useTableColumnCheckBoxes({
    name: 'templateCodes',
    options: checkOptions,
    // @ts-ignore
    defaultValue: templateFieldsRef?.current,
    events: { initOptions: defaultInitOptions },
  });

  const transformExportFieldCodes = useCallback((data, { dataSet }) => {
    data.push(...(dataSet.current?.get('required-option') || []));
    return getExportFieldCodes(data);
  }, []);

  const handleOk = useCallback(async () => {
    const validate = await templateDataSet.validate();
    if (!validate) {
      return false;
    }
    const templateName = templateDataSet.current?.get('templateName');
    const fieldCodes = transformExportFieldCodes(checkBoxDataProps.checkedOptions, checkBoxDataProps);
    if (checkBoxDataProps.checkedOptions.length === 0) {
      Choerodon.prompt('请至少选择一个字段');
      return false;
    }
    const data = {
      ...template,
      name: templateName,
      templateJson: JSON.stringify(uniq(fieldCodes)),
    };
    const newTemplate: ITemplate = await templateApi.edit(template.id, data);
    onEdit(newTemplate);
    modal?.close();
    return false;
  }, [checkBoxDataProps, modal, onEdit, template, templateDataSet, transformExportFieldCodes]);

  useEffect(() => {
  modal?.handleOk(handleOk);
  }, [handleOk, modal]);

  const handleChangeFieldStatus = (status: 'ALL' | 'NONE') => {
    if (status !== 'ALL') {
      checkBoxDataProps.actions.checkAll();
    } else {
      checkBoxDataProps.actions.unCheckAll();
    }
    return true;
  };

  return (
    <div className={styles.template_edit}>
      <FormPart title="修改名称">
        <div className={styles.template_edit_name}>
          <Form dataSet={templateDataSet}>
            <TextField name="templateName" />
          </Form>
        </div>
      </FormPart>

      <FormPart title="修改模板选择字段" btnOnClick={handleChangeFieldStatus}>
        <div className={styles.template_edit_fields}>
          <TableColumnCheckBoxes {...checkBoxComponentProps} />
        </div>
      </FormPart>
    </div>
  );
};

const openEditTemplate = (props: Props) => {
  Modal.open({
    drawer: true,
    key: Modal.key(),
    title: '编辑模板',
    style: {
      width: 380,
    },
    className: styles.editTemplateModal,
    children: <EditTemplate {...props} />,
  });

  // Modal.open({
  //   drawer: true,
  //   key: Modal.key(),
  //   title: '编辑模板',
  //   style: {
  //     width: 380,
  //   },
  //   className: styles.editTemplateModal,
  //   children: (
  //     <span>childred</span>
  //   ),
  //   // children: <ObserverEditTemplate {...props} />,
  // });
};

export default openEditTemplate;