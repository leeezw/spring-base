import { Form, Input, Select, DatePicker, InputNumber, Switch, Radio, Checkbox, Upload, Button, Space, Divider, Row, Col } from 'antd';
import { UploadOutlined, DeleteOutlined, PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import FormField from './FormField.jsx';
import './AdvancedForm.css';

const { TextArea } = Input;
const { Option } = Select;
const { RangePicker } = DatePicker;
const { Group: RadioGroup } = Radio;
const { Group: CheckboxGroup } = Checkbox;

/**
 * 高级表单组件
 * 提供丰富的表单控件和布局选项
 * 
 * @param {Object} props
 * @param {Object} props.form - Form 实例
 * @param {Function} props.onFinish - 提交回调
 * @param {Object} props.initialValues - 初始值
 * @param {Array} props.fields - 字段配置数组
 * @param {string} props.layout - 布局方式 'vertical' | 'horizontal' | 'inline'
 * @param {number} props.colSpan - 列跨度（24栅格系统）
 * @param {boolean} props.showDivider - 是否显示分割线
 * @param {Object} props.dividerConfig - 分割线配置
 */
export default function AdvancedForm({
  form,
  onFinish,
  initialValues = {},
  fields = [],
  layout = 'vertical',
  colSpan = 24,
  showDivider = false,
  dividerConfig = {},
  className = '',
  ...props
}) {
  // 渲染单个字段
  const renderField = (field, index) => {
    const {
      type = 'input',
      name,
      label,
      rules = [],
      required = false,
      placeholder,
      options = [],
      span = colSpan,
      dependencies = [],
      ...fieldProps
    } = field;

    // 根据依赖字段动态显示/隐藏
    if (dependencies.length > 0) {
      const shouldShow = dependencies.every(dep => {
        const depValue = form.getFieldValue(dep.name);
        if (dep.operator === 'equals') {
          return depValue === dep.value;
        } else if (dep.operator === 'notEquals') {
          return depValue !== dep.value;
        } else if (dep.operator === 'in') {
          return Array.isArray(dep.value) && dep.value.includes(depValue);
        }
        return true;
      });

      if (!shouldShow) {
        return null;
      }
    }

    let fieldComponent = null;

    switch (type) {
      case 'input':
        fieldComponent = (
          <Input
            placeholder={placeholder || `请输入${label}`}
            {...fieldProps}
          />
        );
        break;

      case 'textarea':
        fieldComponent = (
          <TextArea
            rows={fieldProps.rows || 4}
            placeholder={placeholder || `请输入${label}`}
            {...fieldProps}
          />
        );
        break;

      case 'password':
        fieldComponent = (
          <Input.Password
            placeholder={placeholder || `请输入${label}`}
            {...fieldProps}
          />
        );
        break;

      case 'number':
        fieldComponent = (
          <InputNumber
            style={{ width: '100%' }}
            placeholder={placeholder || `请输入${label}`}
            {...fieldProps}
          />
        );
        break;

      case 'select':
        fieldComponent = (
          <Select
            placeholder={placeholder || `请选择${label}`}
            allowClear
            {...fieldProps}
          >
            {options.map(option => (
              <Option key={option.value} value={option.value}>
                {option.label}
              </Option>
            ))}
          </Select>
        );
        break;

      case 'date':
        fieldComponent = (
          <DatePicker
            style={{ width: '100%' }}
            placeholder={placeholder || `请选择${label}`}
            {...fieldProps}
          />
        );
        break;

      case 'dateRange':
        fieldComponent = (
          <RangePicker
            style={{ width: '100%' }}
            placeholder={placeholder || ['开始时间', '结束时间']}
            {...fieldProps}
          />
        );
        break;

      case 'switch':
        fieldComponent = (
          <Switch
            checkedChildren={fieldProps.checkedChildren || '是'}
            unCheckedChildren={fieldProps.unCheckedChildren || '否'}
            {...fieldProps}
          />
        );
        break;

      case 'radio':
        fieldComponent = (
          <RadioGroup {...fieldProps}>
            {options.map(option => (
              <Radio key={option.value} value={option.value}>
                {option.label}
              </Radio>
            ))}
          </RadioGroup>
        );
        break;

      case 'checkbox':
        fieldComponent = (
          <CheckboxGroup {...fieldProps}>
            {options.map(option => (
              <Checkbox key={option.value} value={option.value}>
                {option.label}
              </Checkbox>
            ))}
          </CheckboxGroup>
        );
        break;

      case 'upload':
        fieldComponent = (
          <Upload
            {...fieldProps}
            beforeUpload={() => false}
          >
            <Button icon={<UploadOutlined />}>
              {fieldProps.buttonText || '选择文件'}
            </Button>
          </Upload>
        );
        break;

      case 'list':
        fieldComponent = (
          <Form.List name={name}>
            {(fields, { add, remove }) => (
              <>
                {fields.map((field, idx) => (
                  <Space key={field.key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    {fieldProps.fields?.map((item, itemIdx) => (
                      <Form.Item
                        key={itemIdx}
                        {...field}
                        name={[field.name, item.name]}
                        rules={item.rules}
                      >
                        {renderField({ ...item, type: item.type || 'input' }, itemIdx)}
                      </Form.Item>
                    ))}
                    <MinusCircleOutlined
                      onClick={() => remove(field.name)}
                      className="dynamic-delete-button"
                    />
                  </Space>
                ))}
                <Form.Item>
                  <Button
                    type="dashed"
                    onClick={() => add()}
                    block
                    icon={<PlusOutlined />}
                  >
                    {fieldProps.addText || '添加'}
                  </Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        );
        break;

      default:
        fieldComponent = (
          <Input
            placeholder={placeholder || `请输入${label}`}
            {...fieldProps}
          />
        );
    }

    return (
      <FormField
        key={name || index}
        name={name}
        label={label}
        rules={rules}
        required={required}
        {...fieldProps}
      >
        {fieldComponent}
      </FormField>
    );
  };

  // 渲染字段组
  const renderFieldGroup = (group, groupIndex) => {
    if (showDivider && groupIndex > 0) {
      return (
        <div key={groupIndex}>
          <Divider
            orientation={dividerConfig.orientation || 'left'}
            {...dividerConfig}
          >
            {group.title}
          </Divider>
          <Row gutter={16}>
            {group.fields.map((field, index) => (
              <Col span={field.span || colSpan} key={index}>
                {renderField(field, index)}
              </Col>
            ))}
          </Row>
        </div>
      );
    }

    return (
      <Row gutter={16} key={groupIndex}>
        {group.fields.map((field, index) => (
          <Col span={field.span || colSpan} key={index}>
            {renderField(field, index)}
          </Col>
        ))}
      </Row>
    );
  };

  // 判断是否为字段组
  const isFieldGroup = (item) => {
    return Array.isArray(item.fields);
  };

  return (
    <Form
      form={form}
      layout={layout}
      initialValues={initialValues}
      onFinish={onFinish}
      className={`advanced-form ${className}`}
      {...props}
    >
      {fields.map((item, index) => {
        if (isFieldGroup(item)) {
          return renderFieldGroup(item, index);
        } else {
          return (
            <Row gutter={16} key={index}>
              <Col span={item.span || colSpan}>
                {renderField(item, index)}
              </Col>
            </Row>
          );
        }
      })}
    </Form>
  );
}

