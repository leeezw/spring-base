import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Select, Switch, InputNumber, message, Drawer, Divider, Row, Col } from 'antd';
import { 
  ApiOutlined,
  MenuOutlined,
  AppstoreOutlined,
  KeyOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './PermissionList.css';
import { useAuthContext } from '../hooks/AuthProvider.jsx';

const { TreeNode } = Tree;

export default function PermissionList() {
  const [permissionTree, setPermissionTree] = useState([]);
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [selectedKeys, setSelectedKeys] = useState([]);
  const [selectedPermission, setSelectedPermission] = useState(null);
  const [loading, setLoading] = useState(true);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [drawerMode, setDrawerMode] = useState('create');
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [currentParentId, setCurrentParentId] = useState(0);
  const [editingId, setEditingId] = useState(null);
  const { user } = useAuthContext();

  const hasPermission = useCallback((required) => {
    if (!required) {
      return true;
    }
    const requiredList = Array.isArray(required) ? required : [required];
    const permissions = user?.permissions || [];
    if (permissions.includes('*:*:*')) {
      return true;
    }
    return requiredList.some(code => permissions.includes(code));
  }, [user]);

  // 加载权限树
  const loadPermissionTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/system/permission/tree');
      if (res.code === 200 && res.data) {
        const tree = Array.isArray(res.data) ? res.data : [];
        setPermissionTree(tree);
        
        // 自动展开所有节点（如果数据量不大）
        const getAllKeys = (nodes) => {
          const keys = [];
          const traverse = (items) => {
            items.forEach(item => {
              if (item.id) {
                keys.push(item.id.toString());
              }
              if (item.children && item.children.length > 0) {
                traverse(item.children);
              }
            });
          };
          traverse(nodes);
          return keys;
        };
        
        if (tree.length > 0 && tree.length < 100) {
          setExpandedKeys(getAllKeys(tree));
        }
      }
    } catch (error) {
      console.error('fetchPermissions error:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  // 初始加载和筛选变化时重新加载
  useEffect(() => {
    loadPermissionTree();
  }, [loadPermissionTree]);

  // 获取权限类型图标
  const getTypeIcon = (type) => {
    const iconMap = {
      1: <MenuOutlined />,
      2: <AppstoreOutlined />,
      3: <ApiOutlined />,
      'menu': <MenuOutlined />,
      'button': <AppstoreOutlined />,
      'api': <ApiOutlined />,
    };
    return iconMap[type] || <KeyOutlined />;
  };

  // 获取权限类型文本
  const getTypeText = (type) => {
    const typeMap = {
      1: '菜单',
      2: '按钮',
      3: '接口',
      'menu': '菜单',
      'button': '按钮',
      'api': '接口',
    };
    return typeMap[type] || type || '-';
  };

  // 渲染树节点
  const renderTreeNodes = (nodes) => {
    return nodes.map(node => {
      const isEnabled = node.status === 1;
      const title = (
        <div className="tree-node-content">
          <span className="tree-node-icon">{getTypeIcon(node.permissionType)}</span>
          <span className="tree-node-name">{node.permissionName}</span>
          <span className="tree-node-code">({node.permissionCode})</span>
          <Tag color={isEnabled ? 'success' : 'error'} className="tree-node-status">
            {isEnabled ? '启用' : '禁用'}
          </Tag>
        </div>
      );

      return (
        <TreeNode
          key={node.id}
          title={title}
        >
          {node.children && node.children.length > 0 && renderTreeNodes(node.children)}
        </TreeNode>
      );
    });
  };

  // 处理树节点选择
  const handleSelect = (selectedKeys, info) => {
    setSelectedKeys(selectedKeys);
    if (selectedKeys.length > 0 && info.selectedNodes.length > 0) {
      const node = info.selectedNodes[0];
      // 从树节点中找到对应的权限数据
      const findPermission = (nodes, key) => {
        for (const node of nodes) {
          if (node.id.toString() === key) {
            return node;
          }
          if (node.children && node.children.length > 0) {
            const found = findPermission(node.children, key);
            if (found) return found;
          }
        }
        return null;
      };
      const permission = findPermission(permissionTree, selectedKeys[0]);
      setSelectedPermission(permission);
    } else {
      setSelectedPermission(null);
    }
  };

  // 处理展开/收起
  const handleExpand = (expandedKeys) => {
    setExpandedKeys(expandedKeys);
  };

  const openDrawer = (mode, parentId = 0, permission = null) => {
    if (mode === 'create' && !hasPermission('system:permission:add')) {
      message.warning('暂无新增权限');
      return;
    }
    if (mode === 'edit' && !hasPermission('system:permission:edit')) {
      message.warning('暂无编辑权限');
      return;
    }
    setDrawerMode(mode);
    setCurrentParentId(parentId || 0);
    setEditingId(permission?.id || null);
    if (mode === 'edit' && permission) {
      form.setFieldsValue({
        code: permission.permissionCode,
        name: permission.permissionName,
        type: permission.permissionType || 'menu',
        parentId: permission.parentId || 0,
        path: permission.path || '',
        method: permission.method || '',
        icon: permission.icon || '',
        component: permission.component || '',
        visible: permission.visible !== 0,
        status: permission.status !== 0,
        sort: permission.sort ?? 0,
      });
    } else {
      form.setFieldsValue({
        code: '',
        name: '',
        type: 'menu',
        parentId: parentId || 0,
        path: '',
        method: '',
        icon: '',
        component: '',
        visible: true,
        status: true,
        sort: 0,
      });
    }
    setDrawerVisible(true);
  };

  const handleDrawerClose = () => {
    setDrawerVisible(false);
    setEditingId(null);
    form.resetFields();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        permissionCode: values.code,
        permissionName: values.name,
        permissionType: values.type,
        parentId: values.parentId ?? currentParentId ?? 0,
        path: values.path || null,
        method: values.method || null,
        icon: values.icon || null,
        component: values.component || null,
        visible: values.visible ? 1 : 0,
        status: values.status ? 1 : 0,
        sortOrder: Number(values.sort || 0),
      };
      setSubmitLoading(true);
      if (drawerMode === 'create') {
        await request.post('/system/permission', payload);
        message.success('新增成功');
      } else if (editingId) {
        await request.put('/system/permission', { ...payload, id: editingId });
        message.success('更新成功');
      }
      handleDrawerClose();
      loadPermissionTree();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(error?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = () => {
    if (!selectedPermission) {
      return;
    }
    if (!hasPermission('system:permission:delete')) {
      message.warning('暂无删除权限');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除「${selectedPermission.permissionName}」吗？`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await request.delete(`/system/permission/${selectedPermission.id}`);
          message.success('删除成功');
          setSelectedPermission(null);
          setSelectedKeys([]);
          loadPermissionTree();
        } catch (error) {
          message.error(error?.message || '删除失败');
        }
      },
    });
  };

  return (
    <div className="permission-list-page">
      <div className="permission-toolbar">
        <Space>
          {hasPermission('system:permission:add') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create', 0)}>
              新增顶级菜单
            </Button>
          )}
          {hasPermission('system:permission:add') && (
            <Button
              icon={<PlusOutlined />}
              disabled={!selectedPermission}
              onClick={() => openDrawer('create', selectedPermission?.id || 0)}
            >
              新增子节点
            </Button>
          )}
          {hasPermission('system:permission:edit') && (
            <Button
              icon={<EditOutlined />}
              disabled={!selectedPermission}
              onClick={() => openDrawer('edit', selectedPermission?.parentId || 0, selectedPermission)}
            >
              编辑
            </Button>
          )}
          {hasPermission('system:permission:delete') && (
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={!selectedPermission}
              onClick={handleDelete}
            >
              删除
            </Button>
          )}
        </Space>
      </div>

      {/* 权限树和详情 */}
      <div className="permission-content">
        {/* 左侧权限树 */}
        <div className="tree-section">
          <div className="section-title">权限树</div>
          {loading ? (
            <div style={{ padding: '12px' }}>
              <Skeleton active paragraph={{ rows: 10 }} />
            </div>
          ) : permissionTree.length > 0 ? (
            <Tree
              showLine={{ showLeafIcon: false }}
              showIcon={false}
              expandedKeys={expandedKeys}
              selectedKeys={selectedKeys}
              onSelect={handleSelect}
              onExpand={handleExpand}
              className="permission-tree"
            >
              {renderTreeNodes(permissionTree)}
            </Tree>
          ) : (
            <Empty description="暂无权限数据" />
          )}
        </div>

        {/* 右侧权限详情 */}
        <div className="detail-section">
          <div className="section-title">权限详情</div>
          {loading ? (
            <div style={{ padding: '12px' }}>
              <Skeleton active paragraph={{ rows: 8 }} />
            </div>
          ) : selectedPermission ? (
            <Descriptions column={1} bordered className="permission-detail-descriptions">
              <Descriptions.Item label="权限编码">
                <code>{selectedPermission.permissionCode || '-'}</code>
              </Descriptions.Item>
              <Descriptions.Item label="权限名称">
                {selectedPermission.permissionName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="类型">
                <Space>
                  {getTypeIcon(selectedPermission.permissionType)}
                  {getTypeText(selectedPermission.permissionType)}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="路径">
                {selectedPermission.path ? (
                  <code>{selectedPermission.path}</code>
                ) : (
                  <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="HTTP方法">
                {selectedPermission.method ? (
                  <Tag color={
                    selectedPermission.method === 'GET' ? 'blue' :
                    selectedPermission.method === 'POST' ? 'green' :
                    selectedPermission.method === 'PUT' ? 'orange' :
                    selectedPermission.method === 'DELETE' ? 'red' : 'default'
                  }>
                    {selectedPermission.method}
                  </Tag>
                ) : (
                  <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedPermission.status === 1 ? 'success' : 'error'}>
                  {selectedPermission.status === 1 ? '启用' : '禁用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="排序">
                {selectedPermission.sort !== null && selectedPermission.sort !== undefined ? (
                  selectedPermission.sort
                ) : (
                  <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {selectedPermission.createTime ? (
                  formatDateTime(selectedPermission.createTime)
                ) : (
                  <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {selectedPermission.updateTime ? (
                  formatDateTime(selectedPermission.updateTime)
                ) : (
                  <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                )}
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <Empty description="请选择一个权限查看详情" />
          )}
        </div>
      </div>

      <Drawer
        title={drawerMode === 'edit' ? '编辑权限/菜单' : '新增权限/菜单'}
        placement="right"
        size={560}
        open={drawerVisible}
        onClose={handleDrawerClose}
        extra={(
          <Space>
            <Button onClick={handleDrawerClose}>取消</Button>
            <Button type="primary" loading={submitLoading} onClick={handleSubmit}>
              保存
            </Button>
          </Space>
        )}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            type: 'menu',
            parentId: currentParentId,
            visible: true,
            status: true,
            sort: 0,
          }}
          className="permission-drawer-form"
        >
          <Divider orientation="left">基础信息</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label="权限编码" rules={[{ required: true, message: '请输入权限编码' }]}>
                <Input placeholder="例如 menu:user:list" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="name" label="权限名称" rules={[{ required: true, message: '请输入权限名称' }]}>
                <Input placeholder="请输入权限名称" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
                <Select>
                  <Select.Option value="menu">菜单</Select.Option>
                  <Select.Option value="button">按钮</Select.Option>
                  <Select.Option value="api">接口</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="parentId" label="父级 ID">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">路径配置</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="path" label="路由/URL">
                <Input placeholder="例如 /users 或 /api/users" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="method" label="HTTP 方法">
                <Input placeholder="GET / POST / PUT / DELETE" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">展示配置</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="icon" label="图标">
                <Input placeholder="例如 HomeOutlined" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="component" label="前端组件">
                <Input placeholder="例如 UserList" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="sort" label="排序">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="visible" label="是否显示" valuePropName="checked">
                <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="是否启用" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
