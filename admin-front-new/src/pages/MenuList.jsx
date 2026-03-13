import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Select, Switch, InputNumber, message, Drawer, Divider, Row, Col } from 'antd';
import {
  MenuOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  FileOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './MenuList.css';

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
            if (item.id) keys.push(item.id.toString());
            if (item.children?.length > 0) keys.push(...getAllKeys(item.children));
          });
          return keys;
        };
        if (tree.length < 100) setExpandedKeys(getAllKeys(tree));
      }
    } catch (error) {
      console.error('loadMenuTree error:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadMenuTree(); }, [loadMenuTree]);

  const renderTreeNodes = (nodes) => {
    return nodes.map(node => {
      const hasChildren = node.children && node.children.length > 0;
      const title = (
        <div className="tree-node-content">
          <span className="tree-node-icon">{hasChildren ? <FolderOutlined /> : <FileOutlined />}</span>
          <span className="tree-node-name">{node.menuName}</span>
          <span className="tree-node-path">{node.path}</span>
          {node.visible === 0 && (
            <Tag icon={<EyeInvisibleOutlined />} color="default" className="tree-node-tag">隐藏</Tag>
          )}
          <Tag color={node.status === 1 ? 'success' : 'error'} className="tree-node-tag">
            {node.status === 1 ? '启用' : '禁用'}
          </Tag>
        </div>
      );
      return (
        <TreeNode key={node.id.toString()} title={title}>
          {hasChildren && renderTreeNodes(node.children)}
        </TreeNode>
      );
    });
  };

  const findNode = (nodes, key) => {
    for (const node of nodes) {
      if (node.id.toString() === key) return node;
      if (node.children?.length > 0) {
        const found = findNode(node.children, key);
        if (found) return found;
      }
    }
    return null;
  };

  const handleSelect = (keys, info) => {
    setSelectedKeys(keys);
    if (keys.length > 0) {
      setSelectedMenu(findNode(menuTree, keys[0]));
    } else {
      setSelectedMenu(null);
    }
  };

  const openDrawer = (mode, menu = null) => {
    setDrawerMode(mode);
    setEditingId(menu?.id || null);
    if (mode === 'edit' && menu) {
      form.setFieldsValue({
        menuName: menu.menuName,
        parentId: menu.parentId || 0,
        path: menu.path || '',
        component: menu.component || '',
        icon: menu.icon || '',
        sortOrder: menu.sortOrder ?? 0,
        visible: menu.visible !== 0,
        status: menu.status !== 0,
      });
    } else {
      form.setFieldsValue({
        menuName: '',
        parentId: menu?.id || 0,
        path: '',
        component: '',
        icon: '',
        sortOrder: 0,
        visible: true,
        status: true,
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
        ...values,
        visible: values.visible ? 1 : 0,
        status: values.status ? 1 : 0,
        sortOrder: Number(values.sortOrder || 0),
      };
      setSubmitLoading(true);
      if (drawerMode === 'edit' && editingId) {
        await request.put('/system/menu/update', { ...payload, id: editingId });
        message.success('更新成功');
      } else {
        await request.post('/system/menu/save', payload);
        message.success('新增成功');
      }
      handleDrawerClose();
      loadMenuTree();
    } catch (error) {
      if (error?.errorFields) return;
      message.error(error?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = () => {
    if (!selectedMenu) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除菜单「${selectedMenu.menuName}」吗？${selectedMenu.children?.length ? '该菜单下有子菜单，将一并删除。' : ''}`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await request.delete(`/system/menu/delete/${selectedMenu.id}`);
          message.success('删除成功');
          setSelectedMenu(null);
          setSelectedKeys([]);
          loadMenuTree();
        } catch (error) {
          message.error(error?.message || '删除失败');
        }
      },
    });
  };

  return (
    <div className="menu-list-page">
      <div className="menu-toolbar">
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create')}>
            新增菜单
          </Button>
          <Button icon={<PlusOutlined />} disabled={!selectedMenu} onClick={() => openDrawer('create', selectedMenu)}>
            新增子菜单
          </Button>
          <Button icon={<EditOutlined />} disabled={!selectedMenu} onClick={() => openDrawer('edit', selectedMenu)}>
            编辑
          </Button>
          <Button danger icon={<DeleteOutlined />} disabled={!selectedMenu} onClick={handleDelete}>
            删除
          </Button>
        </Space>
      </div>

      <div className="menu-content">
        <div className="tree-section">
          <div className="section-title">菜单树</div>
          {loading ? (
            <Skeleton active paragraph={{ rows: 10 }} style={{ padding: 12 }} />
          ) : menuTree.length > 0 ? (
            <Tree
              showLine={{ showLeafIcon: false }}
              expandedKeys={expandedKeys}
              selectedKeys={selectedKeys}
              onSelect={handleSelect}
              onExpand={setExpandedKeys}
              className="menu-tree"
            >
              {renderTreeNodes(menuTree)}
            </Tree>
          ) : (
            <Empty description="暂无菜单数据" />
          )}
        </div>

        <div className="detail-section">
          <div className="section-title">菜单详情</div>
          {selectedMenu ? (
            <Descriptions column={1} bordered>
              <Descriptions.Item label="菜单名称">{selectedMenu.menuName}</Descriptions.Item>
              <Descriptions.Item label="路由路径"><code>{selectedMenu.path || '-'}</code></Descriptions.Item>
              <Descriptions.Item label="组件路径"><code>{selectedMenu.component || '-'}</code></Descriptions.Item>
              <Descriptions.Item label="图标">{selectedMenu.icon || '-'}</Descriptions.Item>
              <Descriptions.Item label="排序">{selectedMenu.sortOrder ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="可见">
                <Tag color={selectedMenu.visible === 1 ? 'blue' : 'default'}>
                  {selectedMenu.visible === 1 ? '显示' : '隐藏'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedMenu.status === 1 ? 'success' : 'error'}>
                  {selectedMenu.status === 1 ? '启用' : '禁用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(selectedMenu.createTime)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatDateTime(selectedMenu.updateTime)}</Descriptions.Item>
            </Descriptions>
          ) : (
            <Empty description="请选择一个菜单查看详情" />
          )}
        </div>
      </div>

      <Drawer
        title={drawerMode === 'edit' ? '编辑菜单' : '新增菜单'}
        placement="right"
        size={520}
        open={drawerVisible}
        onClose={handleDrawerClose}
        extra={
          <Space>
            <Button onClick={handleDrawerClose}>取消</Button>
            <Button type="primary" loading={submitLoading} onClick={handleSubmit}>保存</Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Divider orientation="left">基础信息</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="menuName" label="菜单名称" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="菜单名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="parentId" label="父级ID">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
          </Row>
          <Divider orientation="left">路由配置</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="path" label="路由路径" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="/system/xxx" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="component" label="组件路径">
                <Input placeholder="Layout 或 system/xxx/index" />
              </Form.Item>
            </Col>
          </Row>
          <Divider orientation="left">显示配置</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="icon" label="图标">
                <Input placeholder="IconXxx" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sortOrder" label="排序">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="visible" label="是否显示" valuePropName="checked">
                <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
              </Form.Item>
            </Col>
            <Col span={12}>
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
