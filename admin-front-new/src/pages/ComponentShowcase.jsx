import { useState, useRef } from 'react';
import { Card, Tabs, Form, Button, Space, Input, message, Tag } from 'antd';
import { 
  AppstoreOutlined,
  FormOutlined,
  TableOutlined,
  SearchOutlined,
  UserOutlined,
  SafetyOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import TableSearchForm from '../components/TableSearchForm.jsx';
import AdvancedForm from '../components/AdvancedForm.jsx';
import FormField from '../components/FormField.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';
import UserForm from '../components/UserForm.jsx';
import RoleSelectModal from '../components/RoleSelectModal.jsx';
import SidebarMenu from '../components/SidebarMenu.jsx';
import './ComponentShowcase.css';

const { TextArea } = Input;

export default function ComponentShowcase() {
  const [searchForm] = Form.useForm();
  const [advancedForm] = Form.useForm();
  const [basicForm] = Form.useForm();
  const tableActionRef = useRef();
  const [roleModalVisible, setRoleModalVisible] = useState(false);

  // 表格数据
  const mockTableData = [
    { id: 1, name: '示例数据1', status: 1, createTime: new Date().toISOString() },
    { id: 2, name: '示例数据2', status: 0, createTime: new Date().toISOString() },
  ];

  const tableColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
  ];

  const fetchTableData = async (params) => {
    return {
      code: 200,
      data: {
        list: mockTableData,
        total: mockTableData.length,
      },
    };
  };

  // AdvancedForm 字段配置示例
  const advancedFormFields = [
    {
      title: '基本信息',
      fields: [
        {
          type: 'input',
          name: 'name',
          label: '姓名',
          rules: [{ required: true, message: '请输入姓名' }],
          required: true,
          span: 12,
        },
        {
          type: 'select',
          name: 'gender',
          label: '性别',
          options: [
            { label: '男', value: 'male' },
            { label: '女', value: 'female' },
          ],
          span: 12,
        },
      ],
    },
    {
      title: '详细信息',
      fields: [
        {
          type: 'date',
          name: 'birthday',
          label: '生日',
          span: 12,
        },
        {
          type: 'switch',
          name: 'enabled',
          label: '是否启用',
          span: 12,
        },
      ],
    },
  ];

  const componentTabs = [
    {
      key: 'table-search',
      label: (
        <span>
          <SearchOutlined />
          表格搜索表单
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>TableSearchForm - 表格搜索表单组件</h3>
            <p>统一的搜索表单控件，包含关键词搜索、状态筛选、日期范围等功能。</p>
          </div>
          <Card className="showcase-card">
            <TableSearchForm
              form={searchForm}
              initialValues={{ status: 'all' }}
              onFinish={(values) => {
                console.log('搜索表单数据:', values);
                message.info('查看控制台查看表单数据');
              }}
              config={{
                showKeyword: true,
                keywordPlaceholder: '搜索关键词',
                showStatus: true,
                statusOptions: [
                  { label: '全部', value: 'all' },
                  { label: '启用', value: 1 },
                  { label: '禁用', value: 0 },
                ],
                showDateRange: true,
                dateRangePlaceholder: ['开始时间', '结束时间'],
              }}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'advanced-form',
      label: (
        <span>
          <FormOutlined />
          高级表单
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>AdvancedForm - 高级表单组件</h3>
            <p>功能强大的表单组件，支持多种字段类型、字段分组、依赖关系和动态列表。</p>
          </div>
          <Card className="showcase-card">
            <AdvancedForm
              form={advancedForm}
              fields={advancedFormFields}
              showDivider={true}
              dividerConfig={{ titlePlacement: 'left' }}
              onFinish={(values) => {
                console.log('高级表单数据:', values);
                message.success('表单提交成功，查看控制台');
              }}
            />
            <div style={{ marginTop: 'var(--spacing-md)' }}>
              <Space>
                <Button type="primary" onClick={() => advancedForm.submit()}>
                  提交表单
                </Button>
                <Button onClick={() => advancedForm.resetFields()}>
                  重置
                </Button>
              </Space>
            </div>
          </Card>
        </div>
      ),
    },
    {
      key: 'form-field',
      label: (
        <span>
          <FormOutlined />
          表单项
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>FormField - 通用表单项组件</h3>
            <p>基于设计系统的通用表单项组件，提供统一的样式和验证规则。</p>
          </div>
          <Card className="showcase-card">
            <Form form={basicForm} layout="vertical">
              <FormField
                name="username"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
                required
              >
                <Input placeholder="请输入用户名" />
              </FormField>
              <FormField
                name="email"
                label="邮箱"
                rules={[
                  { required: true, message: '请输入邮箱' },
                  { type: 'email', message: '请输入有效的邮箱地址' },
                ]}
                required
              >
                <Input placeholder="请输入邮箱" />
              </FormField>
              <FormField
                name="description"
                label="描述"
              >
                <TextArea rows={4} placeholder="请输入描述" />
              </FormField>
            </Form>
          </Card>
        </div>
      ),
    },
    {
      key: 'pro-table',
      label: (
        <span>
          <TableOutlined />
          高级表格
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>ProTableV2 - 高级表格组件</h3>
            <p>基于 @ant-design/pro-components 的高级表格组件，支持搜索、筛选、排序、分页等功能。</p>
          </div>
          <Card className="showcase-card">
            <ProTableV2
              actionRef={tableActionRef}
              headerTitle="示例表格"
              columns={tableColumns}
              request={fetchTableData}
              rowKey="id"
              search={false}
              toolbar={{
                actions: [
                  <Button key="add" type="primary">
                    新增
                  </Button>,
                ],
              }}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'user-form',
      label: (
        <span>
          <UserOutlined />
          用户表单
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>UserForm - 用户表单组件</h3>
            <p>用于新增和编辑用户的表单组件。</p>
          </div>
          <Card className="showcase-card">
            <UserForm
              form={basicForm}
              onFinish={(values) => {
                console.log('用户表单数据:', values);
                message.success('表单提交成功');
              }}
            />
            <div style={{ marginTop: 'var(--spacing-md)' }}>
              <Space>
                <Button type="primary" onClick={() => basicForm.submit()}>
                  提交
                </Button>
                <Button onClick={() => basicForm.resetFields()}>
                  重置
                </Button>
              </Space>
            </div>
          </Card>
        </div>
      ),
    },
    {
      key: 'role-modal',
      label: (
        <span>
          <SafetyOutlined />
          角色选择弹窗
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>RoleSelectModal - 角色选择弹窗组件</h3>
            <p>用于为用户授予角色的弹窗组件。</p>
          </div>
          <Card className="showcase-card">
            <Button
              type="primary"
              onClick={() => setRoleModalVisible(true)}
            >
              打开角色选择弹窗
            </Button>
            <RoleSelectModal
              visible={roleModalVisible}
              userId={1}
              userName="示例用户"
              currentRoleIds={[1, 2]}
              onCancel={() => setRoleModalVisible(false)}
              onOk={() => {
                message.success('角色授予成功');
                setRoleModalVisible(false);
              }}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'sidebar-menu',
      label: (
        <span>
          <MenuOutlined />
          侧边栏菜单
        </span>
      ),
      children: (
        <div className="showcase-section">
          <div className="showcase-description">
            <h3>SidebarMenu - 侧边栏菜单组件</h3>
            <p>侧边栏导航菜单组件，支持多级菜单和折叠功能。</p>
          </div>
          <Card className="showcase-card">
            <div style={{ maxWidth: '300px', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)' }}>
              <SidebarMenu
                items={[
                  {
                    key: '1',
                    label: '首页',
                    path: '/',
                    icon: <AppstoreOutlined />,
                  },
                  {
                    key: '2',
                    label: '用户管理',
                    path: '/users',
                    icon: <UserOutlined />,
                  },
                  {
                    key: '3',
                    label: '系统设置',
                    path: '/settings',
                    icon: <SafetyOutlined />,
                    children: [
                      {
                        key: '3-1',
                        label: '角色管理',
                        path: '/roles',
                      },
                      {
                        key: '3-2',
                        label: '权限管理',
                        path: '/permissions',
                      },
                    ],
                  },
                ]}
                onItemClick={(item) => {
                  console.log('菜单项点击:', item);
                  message.info(`点击了: ${item.label}`);
                }}
                collapsed={false}
              />
            </div>
          </Card>
        </div>
      ),
    },
  ];

  return (
    <div className="component-showcase-page">
      <div className="showcase-header">
        <h2>组件展示</h2>
        <p className="showcase-subtitle">查看项目中所有可用的组件及其使用示例</p>
      </div>

      <Card className="showcase-container">
        <Tabs
          defaultActiveKey="table-search"
          items={componentTabs}
          className="component-tabs"
        />
      </Card>
    </div>
  );
}

