import React, {
  useMemo, useCallback, useState, useRef, useEffect,
} from 'react';
import { observer } from 'mobx-react-lite';
import {
  Page, Header, Content, Breadcrumb, HeaderButtons,
} from '@choerodon/boot';
import {
  Button, DataSet, TextField, Form,
} from 'choerodon-ui/pro';
import { EmptyPage } from '@choerodon/components';
import { Spin } from 'choerodon-ui';
import { ButtonColor } from 'choerodon-ui/pro/lib/button/enum';
import { FieldType } from 'choerodon-ui/pro/lib/data-set/enum';
import { customReportApi, ICreateData, pageConfigApi } from '@/api';
import { IField } from '@/common/types';
import { useWhyDidYouUpdate } from 'ahooks';

import to from '@/utils/to';
import LINK_URL from '@/constants/LINK_URL';
import pic from './NoData.svg';
import styles from './CustomReport.less';
import Condition from '../components/Condition';
import useReport from '../components/Chart/useReport';
import Chart from '../components/Chart';
import { IChartData } from '../components/Chart/utils';

interface Props {
  chartId?: string
}

export interface IChartRes extends ICreateData {
  id: string,
  customChartData: IChartData[],
}

const CustomReport: React.FC<Props> = (props) => {
  // @ts-ignore
  const chartId = props.match.params.id;
  const [loading, setLoading] = useState<boolean>(false);
  const [mode, setMode] = useState<'create' | 'edit' | 'read'>(chartId ? 'read' : 'create');
  const [lost, setLost] = useState<boolean>(false);
  const [chartRes, setChartRes] = useState<null | IChartRes>(null);
  const [dimension, setDimension] = useState<IField[]>([]);
  const [customChartList, setCustomChartList] = useState<{title: string, path: string }[]>([]);

  useEffect(() => {
    pageConfigApi.load().then((res: { content: IField[]}) => {
      setDimension(res.content.filter((item) => ['single', 'checkbox', 'multiple', 'radio', 'member', 'multiMember'].includes(item.fieldType) && !(item.contexts.length === 1 && item.contexts[0] === 'backlog')));
    });
  }, []);

  useEffect(() => {
    if (chartId) {
      customReportApi.getCustomReports().then((res: IChartRes[]) => {
        if (res.length) {
          setCustomChartList(res.filter((item) => item.id !== chartId).map((item) => ({
            title: item.name,
            path: `/agile/charts/${item.id}`,
          })));
        }
      });
    }
  }, [chartId]);

  const [expand, setExpand] = useState<boolean>(true);
  const checkTitle = useCallback(async (value, name, record) => {
    if (!value || value === chartRes?.name) {
      return true;
    }
    const res = await customReportApi.checkName(value);
    if (!res) {
      return true;
    }
    return '报表标题重复';
  }, [chartRes?.name]);
  const reportDs = useMemo(() => new DataSet({
    autoCreate: true,
    fields: [{
      name: 'title',
      label: '图表标题',
      type: 'string' as FieldType,
      maxLength: 16,
      required: true,
      validator: checkTitle,
    }, {
      name: 'description',
      label: '图表描述',
      type: 'string' as FieldType,
      maxLength: 100,
    }, {
      name: 'type',
      label: '图表类型',
      type: 'string' as FieldType,
      required: true,
    }, {
      name: 'row',
      label: '分析维度(表格行)',
      type: 'string' as FieldType,
      required: true,
    }, {
      name: 'column',
      label: '对比维度(表格列)',
      type: 'string' as FieldType,
      dynamicProps: {
        required: ({ record }) => record?.get('type') === 'stackedBar',
      },
    }, {
      name: 'unit',
      label: '图表单位',
      type: 'string' as FieldType,
      required: true,
    }],
    events: {
      update: ({
        // @ts-ignore
        name, value, oldValue, record,
      }) => {
        if (name === 'type' && oldValue === 'stackedBar') {
          record?.set('column', undefined);
        }
      },
    },
  }), [checkTitle]);

  const handleExpandChange = useCallback(() => {
    setExpand(!expand);
  }, [expand]);

  const refresh = useCallback(async () => {
    if (chartId && dimension.length) {
      setLoading(true);
      const res: IChartRes = await customReportApi.getChartAllDataById(chartId);
      setLoading(false);
      if (!dimension.find((item) => item.code === res.analysisField) || (res.comparedField && !dimension.find((item) => item.code === res.comparedField))) {
        setLost(true);
      } else {
        setChartRes(res);
        reportDs.current?.set('title', res.name);
        reportDs.current?.set('description', res.description);
        reportDs.current?.set('type', res.chartType);
        reportDs.current?.set('row', res.analysisField);
        reportDs.current?.set('column', res.comparedField);
        reportDs.current?.set('unit', res.statisticsType);
      }
    }
  }, [chartId, dimension, reportDs]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const handleClickEdit = useCallback(() => {
    setMode('edit');
  }, []);

  const handleClickDelete = useCallback(async () => {
    if (chartId) {
      await customReportApi.deleteChart(chartId);
      to(LINK_URL.report);
    }
  }, [chartId]);

  const back = useCallback(() => {
    to(LINK_URL.report);
  }, []);

  const chartType = reportDs.current?.get('type');
  const analysisField = reportDs.current?.get('row');
  const comparedField = reportDs.current?.get('column');
  const statisticsType = reportDs.current?.get('unit');
  const maxShow = expand ? 18 : 12;
  const configMemo = useMemo(() => ({
    chartType,
    statisticsType,
    analysisField,
    comparedField,
    searchVO: undefined,
    analysisFieldPredefined: (analysisField && dimension.find((item) => item.code === analysisField)?.system),
    comparedFieldPredefined: comparedField && dimension.find((item) => item.code === comparedField)?.system,
  }), [analysisField, chartType, comparedField, dimension, statisticsType]);
  const [, chartProps] = useReport(configMemo, maxShow);
  const { data } = chartProps;

  const handleSave = useCallback(async () => {
    const validate = await reportDs.validate();
    if (validate) {
      const submitData: ICreateData = {
        searchJson: '',
        name: reportDs.current?.get('title'),
        description: reportDs.current?.get('description'),
        chartType,
        statisticsType,
        analysisField,
        comparedField,
        analysisFieldPredefined: analysisField && dimension.find((item) => item.code === analysisField)?.system as boolean,
        comparedFieldPredefined: comparedField && dimension.find((item) => item.code === comparedField)?.system,
        objectVersionNumber: chartRes?.objectVersionNumber,
      };
      mode === 'create' ? await customReportApi.createChart(submitData) : await customReportApi.updateChart(chartId as string, submitData);
      if (mode === 'create') {
        back();
      } else {
        setMode('read');
        refresh();
      }
    }
    setExpand(true);
  }, [reportDs, chartType, statisticsType, analysisField, comparedField, dimension, chartRes?.objectVersionNumber, mode, chartId, back, refresh]);

  const handleCancel = useCallback(() => {
    if (mode === 'create') {
      reportDs.current?.reset();
      back();
    } else {
      reportDs.current?.set('title', chartRes?.name);
      reportDs.current?.set('description', chartRes?.description);
      reportDs.current?.set('type', chartRes?.chartType);
      reportDs.current?.set('row', chartRes?.analysisField);
      reportDs.current?.set('column', chartRes?.comparedField);
      reportDs.current?.set('unit', chartRes?.statisticsType);
      setMode('read');
    }
  }, [back, chartRes?.analysisField, chartRes?.chartType, chartRes?.comparedField, chartRes?.description, chartRes?.name, chartRes?.statisticsType, mode, reportDs]);

  const handleClickSetting = useCallback(() => {
    if (chartId && lost) {
      setMode('edit');
    }
    setExpand(true);
  }, [chartId, lost]);
  useWhyDidYouUpdate('useWhyDidYouUpdateComponent', chartProps);
  // console.log(chartProps);
  return (
    <Page>
      <Header>
        <HeaderButtons items={mode !== 'read' ? [] : [...(customChartList.length ? [{
          display: true,
          name: '切换报表',
          groupBtnItems: customChartList.map((item) => ({
            display: true,
            name: item.title,
            handler: () => {
              to(item.path);
            },
          })),
        }] : []), {
          display: true,
          name: '编辑报表',
          icon: 'edit-o',
          handler: handleClickEdit,
        }, {
          display: true,
          name: '删除报表',
          icon: 'delete_sweep-o',
          handler: handleClickDelete,
        }, {
          name: '返回',
          icon: 'arrow_back',
          iconOnly: true,
          display: true,
          handler: back,
        }, {
          name: '刷新',
          icon: 'refresh',
          iconOnly: true,
          handler: refresh,
          display: true,
        }]}
        />
      </Header>
      <Breadcrumb />
      <Content style={{ padding: 0 }}>
        <Spin spinning={loading}>
          <div className={styles.addReport}>
            {
            mode !== 'read' && (
              <div className={styles.header}>
                {/* <Form dataSet={reportDs}>
                <TextField name="title" placeholder="图表标题" />
              </Form> */}
                <TextField dataSet={reportDs} name="title" placeholder="图表标题" />
              </div>
            )
          }
            <div className={styles.main}>
              <div className={styles.left}>
                {
                (!data?.length || lost) ? (
                  <EmptyPage
                    image={pic}
                    description={lost ? (
                      <>
                        暂无分析维度，请进行
                        <span
                          style={{
                            color: '#5365EA',
                            cursor: 'pointer',
                          }}
                          role="none"
                          onClick={handleClickSetting}
                        >
                          数据设置
                        </span>
                      </>
                    ) : (
                      <>
                        当前暂无自定义图表，您可以通过
                        <span
                          style={{
                            color: '#5365EA',
                            cursor: 'pointer',
                          }}
                          role="none"
                          onClick={() => {
                            if (!expand) {
                              setExpand(true);
                            }
                          }}
                        >
                          数据设置
                        </span>
                        添加默认筛选项来创建图表。
                      </>
                    )}
                  />
                ) : (
                  <Chart {...chartProps} key={`${chartType}-${statisticsType}-${analysisField}-${comparedField}`} />
                )
              }
              </div>
              {
              mode !== 'read' && (
              <div
                className={styles.right}
                style={{
                  width: expand ? 320 : 'unset',
                }}
              >
                <Button
                  icon={expand ? 'last_page' : 'first_page'}
                  className={styles.expand_btn}
                  onClick={handleExpandChange}
                />
                {
                expand && <Condition reportDs={reportDs} dimension={dimension} />
              }
              </div>
              )
            }
            </div>
            {
            mode !== 'read' && (
            <div className={styles.footer}>
              <Button color={'primary' as ButtonColor} onClick={handleSave}>保存</Button>
              <Button onClick={handleCancel}>取消</Button>
            </div>
            )
          }
          </div>
        </Spin>

      </Content>
    </Page>
  );
};

export default observer(CustomReport);
