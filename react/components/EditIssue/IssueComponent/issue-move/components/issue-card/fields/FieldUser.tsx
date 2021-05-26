import React, { useCallback } from 'react';
import { observer } from 'mobx-react-lite';
import TextEditToggle from '@/components/TextEditTogglePro';
import SelectUser from '@/components/select/pro/select-user';
import UserTag from '@/components/tag/user-tag';
import { FieldCommonProps } from '../Field';

export interface FieldUserProps extends FieldCommonProps {

}

const FieldUser: React.FC<FieldUserProps> = ({
  target, field, onChange, fieldWithValue,
}) => {
  const handleChange = useCallback((value) => {
    onChange && onChange(value?.id, value);
  }, [onChange]);
  return (
    <TextEditToggle
      onSubmit={handleChange}
      mountRenderEditor={false}
      initValue={fieldWithValue?.valueStr}
      submitTrigger={['blur', 'change']}
      editor={() => (
        <SelectUser
          projectId={target.projectId}
          primitiveValue={false}
          extraOptions={fieldWithValue?.valueStr}
        />
      )}
    >
      {fieldWithValue?.valueStr ? (
        <UserTag
          data={fieldWithValue?.valueStr}
        />
      ) : '无'}
    </TextEditToggle>
  );
};

export default observer(FieldUser);
