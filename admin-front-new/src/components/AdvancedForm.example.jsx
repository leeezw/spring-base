/**
 * AdvancedForm 使用示例
 * 
 * 这是一个展示如何使用 AdvancedForm 组件的示例文件
 */

import { Form } from 'antd';
import AdvancedForm from './AdvancedForm.jsx';

// 示例1: 基础表单
export function BasicFormExample() {
  const [form] = Form.useForm();

  const fields = [
    {
      type: 'input',
      name: 'name',
      label: '姓名',
      rules: [{ required: true, message: '请输入姓名' }],
      required: true,
      span: 12,
    },
    {
      type: 'input',
      name: 'email',
      label: '邮箱',
      rules: [
        { required: true, message: '请输入邮箱' },
        { type: 'email', message: '请输入有效的邮箱地址' },
      ],
      required: true,
      span: 12,
    },
    {
      type: 'textarea',
      name: 'description',
      label: '描述',
      span: 24,
    },
  ];

  return (
    <AdvancedForm
      form={form}
      fields={fields}
      onFinish={(values) => {
        console.log('表单数据:', values);
      }}
    />
  );
}

// 示例2: 带字段组的表单
export function GroupedFormExample() {
  const [form] = Form.useForm();

  const fields = [
    {
      title: '基本信息',
      fields: [
        {
          type: 'input',
          name: 'username',
          label: '用户名',
          rules: [{ required: true }],
          required: true,
          span: 12,
        },
        {
          type: 'password',
          name: 'password',
          label: '密码',
          rules: [{ required: true }],
          required: true,
          span: 12,
        },
      ],
    },
    {
      title: '详细信息',
      fields: [
        {
          type: 'select',
          name: 'country',
          label: '国家',
          options: [
            { label: '中国', value: 'CN' },
            { label: '美国', value: 'US' },
          ],
          span: 12,
        },
        {
          type: 'date',
          name: 'birthday',
          label: '生日',
          span: 12,
        },
      ],
    },
  ];

  return (
    <AdvancedForm
      form={form}
      fields={fields}
      showDivider={true}
      dividerConfig={{ orientation: 'left' }}
      onFinish={(values) => {
        console.log('表单数据:', values);
      }}
    />
  );
}

// 示例3: 带依赖关系的表单
export function DependentFormExample() {
  const [form] = Form.useForm();

  const fields = [
    {
      type: 'select',
      name: 'userType',
      label: '用户类型',
      options: [
        { label: '个人', value: 'individual' },
        { label: '企业', value: 'company' },
      ],
      span: 12,
    },
    {
      type: 'input',
      name: 'companyName',
      label: '公司名称',
      span: 12,
      dependencies: [
        { name: 'userType', operator: 'equals', value: 'company' },
      ],
    },
    {
      type: 'input',
      name: 'personalName',
      label: '个人姓名',
      span: 12,
      dependencies: [
        { name: 'userType', operator: 'equals', value: 'individual' },
      ],
    },
  ];

  return (
    <AdvancedForm
      form={form}
      fields={fields}
      onFinish={(values) => {
        console.log('表单数据:', values);
      }}
    />
  );
}

// 示例4: 动态列表表单
export function DynamicListFormExample() {
  const [form] = Form.useForm();

  const fields = [
    {
      type: 'list',
      name: 'items',
      label: '项目列表',
      fields: [
        {
          type: 'input',
          name: 'name',
          label: '名称',
          rules: [{ required: true }],
        },
        {
          type: 'number',
          name: 'quantity',
          label: '数量',
          rules: [{ required: true }],
        },
      ],
      addText: '添加项目',
      span: 24,
    },
  ];

  return (
    <AdvancedForm
      form={form}
      fields={fields}
      onFinish={(values) => {
        console.log('表单数据:', values);
      }}
    />
  );
}

