import React, { Component, Fragment } from 'react';
import { Modal } from 'choerodon-ui';
import PropTypes from 'prop-types';
import BaseEditor from './BaseEditor';

const defaultProps = {
  mode: 'edit',
};

const propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  value: PropTypes.any,
  placeholder: PropTypes.string,
  toolbarHeight: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
  ]),
  style: PropTypes.shape({}),
  bottomBar: PropTypes.bool,
  onChange: PropTypes.func,
  handleDelete: PropTypes.func,
  handleSave: PropTypes.func,
  saveRef: PropTypes.func,
  autoFocus: PropTypes.bool,
  mode: PropTypes.oneOf([
    'edit', 'read',
  ]),
};
class WYSIWYGEditor extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      isFullScreen: false,
    };
    this.oldValue = null;// 全屏弹出之前的值，用来处理取消
    this.value = props.value || props.defaultValue;
  }

  saveRef = name => (ref) => {
    this[name] = ref;
    const { saveRef } = this.props;
    if (saveRef) {
      saveRef(ref);
    }
  }

  handleFullScreenClick = () => {
    this.oldValue = this.Editor.value;
    this.setState({
      isFullScreen: true,
    });
  }

  handleFullScreenCancel = () => {
    this.Editor.setValue(this.oldValue);
    this.value = this.oldValue;
    this.setState({
      isFullScreen: false,
    });
  }

  handleCancel = () => {
    const { handleDelete } = this.props;
    if (handleDelete) {
      handleDelete();
    }
  }

  handleSave = async () => {
    this.setState({      
      loading: true,
    });
    const { handleSave } = this.props;
    if (handleSave) {
      try {
        await handleSave(this.value);
        this.setState({
          isFullScreen: false,
        });
      } finally {
        this.setState({
          loading: false,
        });
      }
    }
  }

  handleChange = (value) => {
    this.value = value;
    const { onChange } = this.props;
    if (onChange) {
      onChange(value);
    }
  }

  render() {
    const {
      toolbarHeight,
      handleDelete,
      handleSave,
      mode,
      defaultValue, value,
      ...restProps
    } = this.props;
    const readOnly = mode === 'read';
    const {
      loading, isFullScreen,
    } = this.state;
    return (
      <Fragment>
        <BaseEditor
          {...restProps}
          value={value || this.value}
          onChange={this.handleChange}
          ref={this.saveRef('Editor')}
          mode={mode}
          readOnly={readOnly}
          onFullScreenClick={this.handleFullScreenClick}
          onCancel={this.handleCancel}
          onSave={this.handleSave}
          loading={loading}          
        />
        {
          isFullScreen && (
            <Modal
              title="描述"
              maskClosable={false}
              visible
              width={1200}
              wrapClassName="c7n-agile-editDescription"
              style={{
                height: '85%',
              }}
              onCancel={this.handleFullScreenCancel}
              onOk={this.handleSave}
            >
              <BaseEditor
                {...restProps}
                value={value || this.value}
                onChange={this.handleChange}
                mode={mode}
                readOnly={readOnly}
                bottomBar={false}
                hideFullScreen
                style={{ height: '100%', marginTop: 20, width: '100%' }}                
              />
            </Modal>
          )
        }
      </Fragment>
    );
  }
}
WYSIWYGEditor.defaultProps = defaultProps;
WYSIWYGEditor.propTypes = propTypes;
export default WYSIWYGEditor;
