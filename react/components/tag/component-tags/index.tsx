import React from 'react';

interface LabelTagsProps {
  style?: React.CSSProperties
  maxTagCount?: number
  data: {
    name: string
  }[]
}
const ComponentTags: React.FC<LabelTagsProps> = ({
  data, maxTagCount = 3, style, ...otherProps
}) => {
  if (!data || !data.length) {
    return <span>无</span>;
  }
  const visibleData = data.slice(0, maxTagCount);
  const hiddenData = data.slice(maxTagCount);
  const compact = data.length > maxTagCount;
  return (
    <div style={{ display: 'inline-flex', ...style }}>
      {visibleData.map((item) => (
        <div
          key={item.name}
          style={{
            height: 24,
            color: '#fff',
            borderRadius: '100px',
            fontSize: '13px',
            padding: '2px 12px',
            background: '#5365EA',
            marginRight: '8px',
            marginTop: '2px',
            marginBottom: '2px',
          }}
        >
          {item.name}
        </div>
      ))}
    </div>
  );
};

export default ComponentTags;
