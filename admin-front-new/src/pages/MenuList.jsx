import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Select, Switch, InputNumber, message, Drawer, Divider, Row, Col, TreeSelect } from 'antd';
import { 
  MenuOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './PermissionList.css'; // 复用权限管理的样式

const { TreeNode } = Tree;

export default function MenuList() {
  const [menuTree, setMenuTree] = useState([]);
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [selectedKeys, setSelectedKeys] = useState([]);
  const [selectedMenu, setSelectedMenu] = useState(null);
  const [loading, setLoading] = useState(true);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [drawerMode, setDrawerMode] = useState('create');
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [currentParentId, setCurrentParentId] = useState(0);
  const [editingId, setEditingId] = useState(null);

  const loadMenuTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/system/menu/tree');
      if (res.code === 200 && res.data) {
        const tree = Array.isArray(res.data) ? res.data : [];
        setMenuTree(tree);
        const getAllKeys = (nodes) => {
          const keys = [];
          nodes.forEach(item => {
            keys.push(item.id.toString());
            if (item.children?.length > 0) keys.push(...getAllKeys(item.children));
          });
          return keys;
        };
        if (tree.length > 0 && tree.length < 100) setExpandedKeys(getAllKeys(tree));
      }
    } catch (e) {
      console.error('loadMenuTree error:', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadMenuTree(); }, [loadMenuTree]);

  const buildMenuTreeSelect = (nodes, excludeId = null) => {
    if (!nodes) return [];
    return nodes.filter(n => n.id !== excludeId).map(n => ({
      value: n.id, title: n.menuName,
      children: n.children?.length > 0 ? buildMenuTreeSelect(n.children, excludeId) : undefined,
    }));
  };

  const renderTreeNodes = (nodes) => nodes.map(node => {
    const title = (
      <div className="tree-node-content">
        <span className="tree-node-icon"><MenuOutlined /></span>
        <span className="tree-node-name">{node.menuName}</span>
        <span className="tree-node-code">({node.path || '-'})</span>
        <Tag color={node.visible !== 0 ? 'success' : 'warning'} className="tree-node-status">
          {node.visible !== 0 ? '可见' : '隐藏'}
        </Tag>
      </div>
    );
    return (
      <TreeNode key={node.id} title={title}>
        {node.children?.length > 0 && renderTreeNodes(node.children)}
      </TreeNode>
    );
  });

  const handleSelect = (keys, info) => {
    setSelectedKeys(keys);
    if (keys.length > 0) {
      const findMenu = (nodes, key) => {
        for (const n of nodes) {
          if (n.id.toString() === key) return n;
          if (n.children?.length > 0) { const f = findMenu(n.children, key); if (f) return f; }
        }
        return null;
      };
      setSelectedMenu(findMenu(menuTree, keys[0]));
    } else {
      setSelectedMenu(null);
    }
  };

  const openDrawer = (mode, parentId = 0, menu = null) => {
    setDrawerMode(mode);
    setCurrentParentId(parentId);
    setEditingId(menu?.id || null);
    if (mode === 'edit' && menu) {
      form.setFieldsValue({
        menuName: menu.menuName, path: menu.path || '', component: menu.component || '',
        icon: menu.icon || '', sortOrder: menu.sortOrder ?? 0,
        visible: menu.visible !== 0, status: menu.status !== 0, parentId: menu.parentId || 0,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ parentId: parentId, visible: true, status: true, sortOrder: 0 });
    }
    setDrawerVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        menuName: values.menuName, path: values.path || null, component: values.component || null,
        icon: values.icon || null, sortOrder: Number(values.sortOrder || 0),
        visible: values.visible ? 1 : 0, status: values.status ? 1 : 0,
        parentId: values.parentId ?? currentParentId ?? 0,
      };
      setSubmitLoading(true);
      if (drawerMode === 'create') {
        await request.post('/system/menu/save', payload);
        message.success('新增成功');
      } else if (editingId) {
        await request.put('/system/menu/update', { ...payload, id: editingId });
        message.success('更新成功');
      }
      setDrawerVisible(false);
      form.resetFields();
      loadMenuTree();
    } catch (e) {
      if (e?.errorFields) return;
      message.error(e?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = () => {
    if (!selectedMenu) return;
    Modal.confirm({
      title: '确认删除', content: `确定要删除「${selectedMenu.menuName}」吗？`,
      okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
      onOk: async () => {
        try {
          await request.delete(`/system/menu/delete/${selectedMenu.id}`);
          message.success('删除成功');
          setSelectedMenu(null); setSelectedKeys([]);
          loadMenuTree();
        } catch (e) { message.error(e?.message || '删除失败'); }
      },
    });
  };

  return (
    <div className="permission-list-page">
      <div className="permission-toolbar">
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create', 0)}>新增顶级菜单</Button>
          <Button icon={<PlusOutlined />} disabled={!selectedMenu} onClick={() => openDrawer('create', selectedMenu?.id || 0)}>新增子菜单</Button>
          <Button icon={<EditOutlined />} disabled={!selectedMenu} onClick={() => openDrawer('edit', selectedMenu?.parentId || 0, selectedMenu)}>编辑</Button>
          <Button danger icon={<DeleteOutlined />} disabled={!selectedMenu} onClick={handleDelete}>删除</Button>
        </Space>
      </div>

      <div className="permission-content">
        <div className="tree-section">
          <div className="section-title">菜单树</div>
          {loading ? <Skeleton active paragraph={{ rows: 10 }} style={{ padding: 12 }} /> :
            menuTree.length > 0 ? (
              <Tree showLine={{ showLeafIcon: false }} expandedKeys={expandedKeys} selectedKeys={selectedKeys}
                onSelect={handleSelect} onExpand={setExpandedKeys} className="permission-tree">
                {renderTreeNodes(menuTree)}
              </Tree>
            ) : <Empty description="暂无菜单数据" />
          }
        </div>

        <div className="detail-section">
          <div className="section-title">菜单详情</div>
          {selectedMenu ? (
            <Descriptions column={1} bordered className="permission-detail-descriptions">
              <Descriptions.Item label="菜单名称">{selectedMenu.menuName || '-'}</Descriptions.Item>
              <Descriptions.Item label="路由路径"><code>{selectedMenu.path || '-'}</code></Descriptions.Item>
              <Descriptions.Item label="前端组件"><code>{selectedMenu.component || '-'}</code></Descriptions.Item>
              <Descriptions.Item label="图标">{selectedMenu.icon || '-'}</Descriptions.Item>
              <Descriptions.Item label="排序">{selectedMenu.sortOrder ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="是否可见">
                <Tag color={selectedMenu.visible !== 0 ? 'success' : 'warning'} icon={selectedMenu.visible !== 0 ? <EyeOutlined /> : <EyeInvisibleOutlined />}>
                  {selectedMenu.visible !== 0 ? '可见' : '隐藏'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedMenu.status === 1 ? 'success' : 'error'}>{selectedMenu.status === 1 ? '启用' : '禁用'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{selectedMenu.createTime ? formatDateTime(selectedMenu.createTime) : '-'}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{selectedMenu.updateTime ? formatDateTime(selectedMenu.updateTime) : '-'}</Descriptions.Item>
            </Descriptions>
          ) : <Empty description="请选择一个菜单查看详情" />}
        </div>
      </div>

      <Drawer title={drawerMode === 'edit' ? '编辑菜单' : '新增菜单'} placement="right" width={480}
        open={drawerVisible} onClose={() => { setDrawerVisible(false); form.resetFields(); }}
        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type="primary" loading={submitLoading} onClick={handleSubmit}>保存</Button></Space>}>
        <Form form={form} layout="vertical">
          <Divider orientation="left" plain>基础信息</Divider>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="menuName" label="菜单名称" rules={[{ required: true, message: '请输入' }]}><Input placeholder="如 用户管理" /></Form.Item></Col>
            <Col span={12}><Form.Item name="parentId" label="上级菜单"><TreeSelect treeData={[{ value: 0, title: '无（顶级菜单）' }, ...buildMenuTreeSelect(menuTree, editingId)]} placeholder="请选择" treeDefaultExpandAll allowClear style={{ width: '100%' }} /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="path" label="路由路径"><Input placeholder="/system/user" /></Form.Item></Col>
            <Col span={12}><Form.Item name="component" label="前端组件"><Input placeholder="UserList" /></Form.Item></Col>
          </Row>
          <Divider orientation="left" plain>展示配置</Divider>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="icon" label="图标"><Input placeholder="IconUser" /></Form.Item></Col>
            <Col span={12}><Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="visible" label="是否可见" valuePropName="checked"><Switch checkedChildren="可见" unCheckedChildren="隐藏" /></Form.Item></Col>
            <Col span={12}><Form.Item name="status" label="是否启用" valuePropName="checked"><Switch checkedChildren="启用" unCheckedChildren="禁用" /></Form.Item></Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
