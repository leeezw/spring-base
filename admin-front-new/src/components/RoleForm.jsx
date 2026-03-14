import { useState, useEffect } from 'react';
import { Form, Input, Select, Tree, Skeleton, TreeSelect, Divider } from 'antd';
import { 
  ApiOutlined,
  MenuOutlined,
  AppstoreOutlined,
  KeyOutlined,
  ApartmentOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import './UserForm.css';

const { Option } = Select;

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门数据' },
  { value: 3, label: '本部门及下级数据' },
  { value: 4, label: '仅本人数据' },
  { value: 5, label: '自定义部门' },
];

/**
 * 角色表单组件
 */
export default function RoleForm({ form, initialValues, onFinish, permissionTree = [], permissionTreeLoading = false }) {
  const isEdit = !!initialValues;
  const [checkedKeys, setCheckedKeys] = useState([]);
  const [dataScope, setDataScope] = useState(1);
  const [deptTree, setDeptTree] = useState([]);
  const [deptLoading, setDeptLoading] = useState(false);

  useEffect(() => {
    const permissionIds = form.getFieldValue('permissionIds') || [];
    const keys = permissionIds.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id));
    setCheckedKeys(keys);
    setDataScope(form.getFieldValue('dataScope') || initialValues?.dataScope || 1);
  }, [form, initialValues]);

  // 加载部门树
  useEffect(() => {
    const loadDeptTree = async () => {
      setDeptLoading(true);
      try {
        const res = await request.get('/system/dept/tree');
        if (res.code === 200) setDeptTree(res.data || []);
      } catch (e) { /* ignore */ }
      finally { setDeptLoading(false); }
    };
    loadDeptTree();
  }, []);

  // 加载角色已关联的自定义部门
  useEffect(() => {
    if (isEdit && initialValues?.id) {
      request.get(`/system/relation/role/${initialValues.id}/depts`).then(res => {
        if (res.code === 200 && res.data) {
          form.setFieldsValue({ customDeptIds: res.data });
        }
      }).catch(() => {});
    }
  }, [isEdit, initialValues?.id, form]);

  const getTypeIcon = (type) => {
    const iconMap = { 1: <MenuOutlined />, 2: <AppstoreOutlined />, 3: <ApiOutlined /> };
    return iconMap[type] || <KeyOutlined />;
  };

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

  const buildDeptTreeSelect = (nodes) => {
    if (!nodes) return [];
    return nodes.map(n => ({
      value: n.id,
      title: n.deptName,
      children: n.children?.length > 0 ? buildDeptTreeSelect(n.children) : undefined,
    }));
  };

  const handlePermissionChange = (checkedKeysValue) => {
    const keys = Array.isArray(checkedKeysValue) ? checkedKeysValue : (checkedKeysValue?.checked || []);
    setCheckedKeys(keys);
    form.setFieldsValue({ permissionIds: keys.map(id => typeof id === 'string' ? parseInt(id) : id).filter(id => !isNaN(id)) });
  };

  const handleDataScopeChange = (value) => {
    setDataScope(value);
    form.setFieldsValue({ dataScope: value });
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{ status: 1, dataScope: 1, permissionIds: [], customDeptIds: [], ...initialValues }}
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

      <Divider orientation="left" plain style={{ fontSize: 13, color: '#8c8c8c' }}>
        <ApartmentOutlined /> 数据权限
      </Divider>

      <Form.Item
        name="dataScope"
        label="数据范围"
        tooltip="控制该角色用户能看到哪些部门的数据"
      >
        <Select options={DATA_SCOPE_OPTIONS} onChange={handleDataScopeChange} />
      </Form.Item>

      {dataScope === 5 && (
        <Form.Item
          name="customDeptIds"
          label="自定义部门"
          rules={[{ required: true, message: '请选择至少一个部门' }]}
        >
          <TreeSelect
            treeData={buildDeptTreeSelect(deptTree)}
            placeholder="选择可访问的部门"
            treeCheckable
            showCheckedStrategy={TreeSelect.SHOW_ALL}
            treeDefaultExpandAll
            loading={deptLoading}
            style={{ width: '100%' }}
          />
        </Form.Item>
      )}

      <Divider orientation="left" plain style={{ fontSize: 13, color: '#8c8c8c' }}>
        <KeyOutlined /> 功能权限
      </Divider>

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
