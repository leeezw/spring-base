import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Switch, InputNumber, message, Drawer, Divider, Row, Col } from 'antd';
import {
  ApartmentOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  BankOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './DeptList.css';

const { TreeNode } = Tree;

export default function DeptList() {
  const [deptTree, setDeptTree] = useState([]);
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [selectedKeys, setSelectedKeys] = useState([]);
  const [selectedDept, setSelectedDept] = useState(null);
  const [loading, setLoading] = useState(true);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [drawerMode, setDrawerMode] = useState('create');
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const loadDeptTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/system/dept/tree');
      if (res.code === 200 && res.data) {
        const tree = Array.isArray(res.data) ? res.data : [];
        setDeptTree(tree);
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
      console.error('loadDeptTree error:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadDeptTree(); }, [loadDeptTree]);

  const renderTreeNodes = (nodes) => {
    return nodes.map(node => {
      const hasChildren = node.children && node.children.length > 0;
      const title = (
        <div className="tree-node-content">
          <span className="tree-node-icon">{hasChildren ? <BankOutlined /> : <TeamOutlined />}</span>
          <span className="tree-node-name">{node.deptName}</span>
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

  const handleSelect = (keys) => {
    setSelectedKeys(keys);
    setSelectedDept(keys.length > 0 ? findNode(deptTree, keys[0]) : null);
  };

  const openDrawer = (mode, dept = null) => {
    setDrawerMode(mode);
    setEditingId(dept?.id || null);
    if (mode === 'edit' && dept) {
      form.setFieldsValue({
        deptName: dept.deptName,
        parentId: dept.parentId || 0,
        phone: dept.phone || '',
        email: dept.email || '',
        sortOrder: dept.sortOrder ?? 0,
        status: dept.status !== 0,
      });
    } else {
      form.setFieldsValue({
        deptName: '',
        parentId: mode === 'create' && dept ? dept.id : 0,
        phone: '',
        email: '',
        sortOrder: 0,
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
        status: values.status ? 1 : 0,
        sortOrder: Number(values.sortOrder || 0),
      };
      setSubmitLoading(true);
      if (drawerMode === 'edit' && editingId) {
        await request.put('/system/dept', { ...payload, id: editingId });
        message.success('更新成功');
      } else {
        await request.post('/system/dept', payload);
        message.success('新增成功');
      }
      handleDrawerClose();
      loadDeptTree();
    } catch (error) {
      if (error?.errorFields) return;
      message.error(error?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = () => {
    if (!selectedDept) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除部门「${selectedDept.deptName}」吗？${selectedDept.children?.length ? '该部门下有子部门，将一并删除。' : ''}`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await request.delete(`/system/dept/${selectedDept.id}`);
          message.success('删除成功');
          setSelectedDept(null);
          setSelectedKeys([]);
          loadDeptTree();
        } catch (error) {
          message.error(error?.message || '删除失败');
        }
      },
    });
  };

  return (
    <div className="dept-list-page">
      <div className="dept-toolbar">
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create')}>
            新增部门
          </Button>
          <Button icon={<PlusOutlined />} disabled={!selectedDept} onClick={() => openDrawer('create', selectedDept)}>
            新增子部门
          </Button>
          <Button icon={<EditOutlined />} disabled={!selectedDept} onClick={() => openDrawer('edit', selectedDept)}>
            编辑
          </Button>
          <Button danger icon={<DeleteOutlined />} disabled={!selectedDept} onClick={handleDelete}>
            删除
          </Button>
        </Space>
      </div>

      <div className="dept-content">
        <div className="tree-section">
          <div className="section-title">
            <ApartmentOutlined style={{ marginRight: 8 }} />
            组织架构
          </div>
          {loading ? (
            <Skeleton active paragraph={{ rows: 10 }} style={{ padding: 12 }} />
          ) : deptTree.length > 0 ? (
            <Tree
              showLine={{ showLeafIcon: false }}
              expandedKeys={expandedKeys}
              selectedKeys={selectedKeys}
              onSelect={handleSelect}
              onExpand={setExpandedKeys}
              className="dept-tree"
            >
              {renderTreeNodes(deptTree)}
            </Tree>
          ) : (
            <Empty description="暂无部门数据" />
          )}
        </div>

        <div className="detail-section">
          <div className="section-title">部门详情</div>
          {selectedDept ? (
            <Descriptions column={1} bordered>
              <Descriptions.Item label="部门名称">{selectedDept.deptName}</Descriptions.Item>
              <Descriptions.Item label="联系电话">{selectedDept.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{selectedDept.email || '-'}</Descriptions.Item>
              <Descriptions.Item label="排序">{selectedDept.sortOrder ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedDept.status === 1 ? 'success' : 'error'}>
                  {selectedDept.status === 1 ? '启用' : '禁用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(selectedDept.createTime)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatDateTime(selectedDept.updateTime)}</Descriptions.Item>
            </Descriptions>
          ) : (
            <Empty description="请选择一个部门查看详情" />
          )}
        </div>
      </div>

      <Drawer
        title={drawerMode === 'edit' ? '编辑部门' : '新增部门'}
        placement="right"
        size={480}
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
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input placeholder="请输入部门名称" />
          </Form.Item>
          <Form.Item name="parentId" label="上级部门ID">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="phone" label="联系电话">
                <Input placeholder="联系电话" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="email" label="邮箱">
                <Input placeholder="邮箱" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sortOrder" label="排序">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
