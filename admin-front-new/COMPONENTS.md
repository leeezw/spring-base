# 通用组件和页面使用文档

本文档介绍项目中新增的通用组件和页面的使用方法。

## 📦 新增组件

### 1. AdvancedForm - 高级表单组件

一个功能强大的表单组件，支持多种字段类型、字段分组、依赖关系和动态列表。

#### 基本用法

```jsx
import { Form } from 'antd';
import AdvancedForm from '../components/AdvancedForm.jsx';

function MyForm() {
  const [form] = Form.useForm();

  const fields = [
    {
      type: 'input',
      name: 'username',
      label: '用户名',
      rules: [{ required: true, message: '请输入用户名' }],
      required: true,
      span: 12,
    },
    {
      type: 'password',
      name: 'password',
      label: '密码',
      rules: [{ required: true, message: '请输入密码' }],
      required: true,
      span: 12,
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
```

#### 支持的字段类型

- `input` - 文本输入框
- `textarea` - 多行文本输入
- `password` - 密码输入框
- `number` - 数字输入框
- `select` - 下拉选择框
- `date` - 日期选择器
- `dateRange` - 日期范围选择器
- `switch` - 开关
- `radio` - 单选按钮组
- `checkbox` - 复选框组
- `upload` - 文件上传
- `list` - 动态列表

#### 字段分组

```jsx
const fields = [
  {
    title: '基本信息',
    fields: [
      { type: 'input', name: 'name', label: '姓名', span: 12 },
      { type: 'input', name: 'email', label: '邮箱', span: 12 },
    ],
  },
  {
    title: '详细信息',
    fields: [
      { type: 'select', name: 'country', label: '国家', span: 12 },
    ],
  },
];

<AdvancedForm
  form={form}
  fields={fields}
  showDivider={true}
  dividerConfig={{ orientation: 'left' }}
/>
```

#### 字段依赖

```jsx
const fields = [
  {
    type: 'select',
    name: 'userType',
    label: '用户类型',
    options: [
      { label: '个人', value: 'individual' },
      { label: '企业', value: 'company' },
    ],
  },
  {
    type: 'input',
    name: 'companyName',
    label: '公司名称',
    dependencies: [
      { name: 'userType', operator: 'equals', value: 'company' },
    ],
  },
];
```

#### 动态列表

```jsx
const fields = [
  {
    type: 'list',
    name: 'items',
    label: '项目列表',
    fields: [
      { type: 'input', name: 'name', label: '名称' },
      { type: 'number', name: 'quantity', label: '数量' },
    ],
    addText: '添加项目',
  },
];
```

## 📄 新增页面

### 1. NotificationCenter - 通知消息中心

通知消息管理页面，支持查看、筛选、标记已读和删除通知。

**路由**: `/notifications`

**功能特性**:
- 通知列表展示
- 按类型筛选（全部、未读、信息、成功、警告、错误）
- 关键词搜索
- 标记已读/全部已读
- 删除通知
- 清空已读通知

**使用示例**:
```jsx
// 已在路由中配置，直接访问 /notifications 即可
```

### 2. Profile - 个人中心

个人中心页面，用于管理个人信息和账户设置。

**路由**: `/profile`

**功能特性**:
- 查看和编辑个人信息
- 上传和更换头像
- 修改密码
- 账户安全设置

**使用示例**:
```jsx
// 已在路由中配置，直接访问 /profile 即可
```

## 🎨 样式系统

所有组件和页面都遵循项目的设计系统规范：

- **颜色**: 使用 CSS 变量（`--primary-color`, `--text-primary` 等）
- **间距**: 使用间距系统（`--spacing-xs`, `--spacing-sm` 等）
- **圆角**: 使用圆角系统（`--radius-sm`, `--radius-md` 等）
- **阴影**: 使用阴影系统（`--shadow-sm`, `--shadow-md` 等）
- **字体**: 使用字体系统（`--font-family`, `--font-size-sm` 等）

## 📝 注意事项

1. **AdvancedForm** 组件需要传入 `form` 实例（通过 `Form.useForm()` 创建）
2. **字段配置** 中的 `span` 属性使用 24 栅格系统
3. **依赖关系** 支持 `equals`, `notEquals`, `in` 等操作符
4. **通知中心** 目前使用模拟数据，需要对接实际的后端 API
5. **个人中心** 需要确保用户已登录，会从 `AuthProvider` 获取用户信息

## 🔗 相关文件

- `src/components/AdvancedForm.jsx` - 高级表单组件
- `src/components/AdvancedForm.css` - 高级表单样式
- `src/components/AdvancedForm.example.jsx` - 使用示例
- `src/pages/NotificationCenter.jsx` - 通知中心页面
- `src/pages/NotificationCenter.css` - 通知中心样式
- `src/pages/Profile.jsx` - 个人中心页面
- `src/pages/Profile.css` - 个人中心样式

