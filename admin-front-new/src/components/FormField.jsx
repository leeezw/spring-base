import { Form } from 'antd';
import './FormField.css';

/**
 * 通用表单项组件
 * 基于登录页面的表单规范
 */
export default function FormField({ 
  label, 
  name, 
  rules, 
  children, 
  required = false,
  ...props 
}) {
  return (
    <Form.Item
      name={name}
      label={label}
      rules={rules}
      required={required}
      className="form-field"
      {...props}
    >
      {children}
    </Form.Item>
  );
}

