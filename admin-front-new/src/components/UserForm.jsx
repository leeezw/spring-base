import { Form, Input, Select } from 'antd';
import FormField from './FormField.jsx';
import './UserForm.css';

const { Option } = Select;

/**
 * 用户表单组件
 * 用于新增和编辑用户
 */
export default function UserForm({ form, initialValues, onFinish }) {
  const isEdit = !!initialValues;

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{ status: 1, ...initialValues }}
      onFinish={onFinish}
      className="user-form"
    >
      <FormField
        name="username"
        label="用户名"
        rules={[
          { required: true, message: '请输入用户名' },
          { min: 3, message: '用户名至少3个字符' },
          { max: 20, message: '用户名最多20个字符' },
        ]}
        required
      >
        <Input placeholder="请输入用户名" disabled={isEdit} />
      </FormField>

      <FormField
        name="password"
        label="初始密码"
        rules={[
          { required: !isEdit, message: '请输入密码' },
          { min: 6, message: '密码至少6个字符' },
        ]}
        required={!isEdit}
      >
        <Input.Password placeholder={isEdit ? '留空则不修改密码' : '请输入密码'} />
      </FormField>

      <FormField
        name="nickname"
        label="昵称"
        rules={[
          { max: 50, message: '昵称最多50个字符' },
        ]}
      >
        <Input placeholder="请输入昵称" />
      </FormField>

      <FormField
        name="email"
        label="邮箱"
        rules={[
          { type: 'email', message: '请输入有效的邮箱地址' },
        ]}
      >
        <Input placeholder="请输入邮箱" />
      </FormField>

      {/* 状态字段只在新增时显示，编辑时通过独立的状态按钮修改 */}
      {!isEdit && (
        <FormField
          name="status"
          label="状态"
        >
          <Select>
            <Option value={1}>启用</Option>
            <Option value={0}>禁用</Option>
          </Select>
        </FormField>
      )}
    </Form>
  );
}

