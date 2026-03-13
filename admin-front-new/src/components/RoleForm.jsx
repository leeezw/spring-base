import { useState, useEffect } from 'react';
import { Form, Input, Select, Tree, Skeleton } from 'antd';
import { 
  ApiOutlined,
  MenuOutlined,
  AppstoreOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import './UserForm.css';

const { Option } = Select;

/**
 * 角色表单组件
 */
export default function RoleForm({ form, initialValues, onFinish, permissionTree = [], permissionTreeLoading = false }) {
  const isEdit = !!initialValues;
  const [checkedKeys, setCheckedKeys] = useState([]);

  useEffect(() => {
    const permissionIds = form.getFieldValue('permissionIds') || [];
    const keys = permissionIds.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id));
    setCheckedKeys(keys);
  }, [form, initialValues]);

  const getTypeIcon = (type) => {
    const iconMap = { 1: <MenuOutlined />, 2: <AppstoreOutlined />, 3: <ApiOutlined /> };
    return iconMap[type] || <KeyOutlined />;
  };

  // 后端字段: permissionName, permissionCode, permissionType
  const buildTreeData = (nodes) => {
    if (!nodes) return [];
    return nodes.map(node => ({
      key: node.id,
      title: (
        <span>
          {getTypeIcon(node.permissionType)}
          <span style={{ marginLeft: 6 }}>{node.permissionName}</span>
          <span style={{ marginLeft: 6, color: '#999', fontSize: 12 }}>({node.permissionCode})</span>
        </span>
      ),
      children: node.children?.length > 0 ? buildTreeData(node.children) : undefined,
    }));
  };

  const handlePermissionChange = (checkedKeysValue) => {
    const keys = Array.isArray(checkedKeysValue) ? checkedKeysValue : (checkedKeysValue?.checked || []);
    setCheckedKeys(keys);
    form.setFieldsValue({ permissionIds: keys.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id)) });
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{ status: 1, permissionIds: [], ...initialValues }}
      onFinish={onFinish}
      className="user-form"
    >
      <Form.Item
        name="code"
        label="角色编码"
        rules={[
          { required: true, message: '请输入角色编码' },
          { pattern: /^[A-Za-z0-9_]+$/, message: '只能包含字母、数字和下划线' },
        ]}
      >
        <Input placeholder="如 ROLE_ADMIN" disabled={isEdit} />
      </Form.Item>

      <Form.Item
        name="name"
        label="角色名称"
        rules={[{ required: true, message: '请输入角色名称' }]}
      >
        <Input placeholder="如 超级管理员" />
      </Form.Item>

      <Form.Item name="description" label="描述">
        <Input.TextArea placeholder="角色描述" rows={2} />
      </Form.Item>

      {!isEdit && (
        <Form.Item name="status" label="状态">
          <Select>
            <Option value={1}>启用</Option>
            <Option value={0}>禁用</Option>
          </Select>
        </Form.Item>
      )}

      <Form.Item name="permissionIds" label="权限配置">
        {permissionTreeLoading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : (
          <Tree
            checkable
            treeData={buildTreeData(permissionTree)}
            checkedKeys={checkedKeys}
            onCheck={handlePermissionChange}
            defaultExpandAll
            style={{
              maxHeight: 400,
              overflowY: 'auto',
              border: '1px solid #d9d9d9',
              borderRadius: 8,
              padding: 12,
            }}
          />
        )}
      </Form.Item>
    </Form>
  );
}
