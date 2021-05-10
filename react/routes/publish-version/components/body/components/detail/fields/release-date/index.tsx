import React, { useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { DatePicker } from 'choerodon-ui/pro/lib';
import TextEditToggle from '@/components/TextEditTogglePro';
import moment, { Moment } from 'moment';
import { usePublishVersionContext } from '@/routes/publish-version/stores';
import Field from '../field';

interface Props {

}
const BusinessValue: React.FC<Props> = () => {
  const { store } = usePublishVersionContext();
  const { actualPublishDate: originData } = store.getCurrentData;
  const actualPublishDate = useMemo(() => (originData ? moment(originData, 'YYYY-MM-DD HH:mm:ss').format('YYYY-MM-DD HH:mm:ss') : undefined), [originData]);

  return (

    <Field label="发布时间">
      <TextEditToggle
        onSubmit={(value: Moment | null) => {
          store.update('actualPublishDate', value?.format('YYYY-MM-DD HH:mm:ss'));
        }}
        // disabled={disabled}
        initValue={actualPublishDate ? moment(actualPublishDate, 'YYYY-MM-DD') : undefined}
        submitTrigger={['blur', 'change']}
        editor={() => (
          <DatePicker style={{ width: '100%' }} />
        )}
      >
        {actualPublishDate || '无'}
      </TextEditToggle>
    </Field>
  );
};

export default observer(BusinessValue);