import { useState, useEffect } from 'react';
import { Form, Input, Select, Tree, Skeleton } from 'antd';
import { 
  ApiOutlined,
  MenuOutlined,
  AppstoreOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import FormField from './FormField.jsx';
import './UserForm.css';

const { Option } = Select;
const { TreeNode } = Tree;

/**
 * 角色表单组件
 * 用于新增和编辑角色
 */
export default function RoleForm({ form, initialValues, onFinish, permissionTree = [], permissionTreeLoading = false }) {
  const isEdit = !!initialValues;
  const [checkedKeys, setCheckedKeys] = useState([]);

  // 监听表单值变化，更新选中的权限
  useEffect(() => {
    const permissionIds = form.getFieldValue('permissionIds') || [];
    // 确保权限ID是数字类型（Tree组件需要）
    const keys = permissionIds.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id));
    setCheckedKeys(keys);
  }, [form, initialValues]);

  // 获取权限类型图标
  const getTypeIcon = (type) => {
    const iconMap = {
      'menu': <MenuOutlined />,
      'button': <AppstoreOutlined />,
      'api': <ApiOutlined />,
    };
    return iconMap[type] || <KeyOutlined />;
  };

  // 渲染权限树节点
  const renderPermissionNodes = (nodes) => {
    return nodes.map(node => {
      const title = (
        <span>
          {getTypeIcon(node.type)}
          <span style={{ marginLeft: 8 }}>{node.name}</span>
          <span style={{ marginLeft: 8, color: '#999', fontSize: '12px' }}>({node.code})</span>
        </span>
      );

      return (
        <TreeNode
          key={node.id}
          title={title}
          value={node.id}
        >
          {node.children && node.children.length > 0 && renderPermissionNodes(node.children)}
        </TreeNode>
      );
    });
  };

  // 处理权限选择变化
  const handlePermissionChange = (checkedKeysValue) => {
    setCheckedKeys(checkedKeysValue);
    // 更新表单值，确保是数字数组
    const ids = Array.isArray(checkedKeysValue) 
      ? checkedKeysValue.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id))
      : [];
    form.setFieldsValue({ permissionIds: ids });
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{ status: 1, permissionIds: [], ...initialValues }}
      onFinish={onFinish}
      className="user-form"
    >
      <FormField
        name="code"
        label="角色编码"
        rules={[
          { required: true, message: '请输入角色编码' },
          { min: 2, message: '角色编码至少2个字符' },
          { max: 50, message: '角色编码最多50个字符' },
          { pattern: /^[A-Za-z0-9_]+$/, message: '角色编码只能包含字母、数字和下划线' },
        ]}
        required
      >
        <Input placeholder="请输入角色编码" disabled={isEdit} />
      </FormField>

      <FormField
        name="name"
        label="角色名称"
        rules={[
          { required: true, message: '请输入角色名称' },
          { min: 2, message: '角色名称至少2个字符' },
          { max: 50, message: '角色名称最多50个字符' },
        ]}
        required
      >
        <Input placeholder="请输入角色名称" />
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

      {/* 权限选择 */}
      <FormField
        name="permissionIds"
        label="权限配置"
      >
        {permissionTreeLoading ? (
          <div
            style={{
              border: '1px solid #d9d9d9',
              borderRadius: '4px',
              padding: '12px',
              minHeight: '200px',
            }}
          >
            <Skeleton active paragraph={{ rows: 8 }} />
          </div>
        ) : (
          <Tree
            checkable
            checkStrictly={false}
            defaultExpandAll={permissionTree.length < 50}
            checkedKeys={checkedKeys}
            onCheck={handlePermissionChange}
            style={{
              maxHeight: '400px',
              overflowY: 'auto',
              border: '1px solid #d9d9d9',
              borderRadius: '4px',
              padding: '12px',
            }}
          >
            {renderPermissionNodes(permissionTree)}
          </Tree>
        )}
      </FormField>
    </Form>
  );
}

