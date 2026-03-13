import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Switch, InputNumber, message, Drawer, Divider, Row, Col } from 'antd';
import { 
  ApartmentOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PhoneOutlined,
  MailOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './PermissionList.css'; // 复用权限管理的样式

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
  const [currentParentId, setCurrentParentId] = useState(0);
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
            keys.push(item.id.toString());
            if (item.children?.length > 0) keys.push(...getAllKeys(item.children));
          });
          return keys;
        };
        if (tree.length > 0) setExpandedKeys(getAllKeys(tree));
      }
    } catch (e) {
      console.error('loadDeptTree error:', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadDeptTree(); }, [loadDeptTree]);

  const renderTreeNodes = (nodes) => nodes.map(node => {
    const title = (
      <div className="tree-node-content">
        <span className="tree-node-icon"><ApartmentOutlined /></span>
        <span className="tree-node-name">{node.deptName}</span>
        <Tag color={node.status === 1 ? 'success' : 'error'} className="tree-node-status">
          {node.status === 1 ? '启用' : '禁用'}
        </Tag>
      </div>
    );
    return (
      <TreeNode key={node.id} title={title}>
        {node.children?.length > 0 && renderTreeNodes(node.children)}
      </TreeNode>
    );
  });

  const handleSelect = (keys) => {
    setSelectedKeys(keys);
    if (keys.length > 0) {
      const findDept = (nodes, key) => {
        for (const n of nodes) {
          if (n.id.toString() === key) return n;
          if (n.children?.length > 0) { const f = findDept(n.children, key); if (f) return f; }
        }
        return null;
      };
      setSelectedDept(findDept(deptTree, keys[0]));
    } else {
      setSelectedDept(null);
    }
  };

  const openDrawer = (mode, parentId = 0, dept = null) => {
    setDrawerMode(mode);
    setCurrentParentId(parentId);
    setEditingId(dept?.id || null);
    if (mode === 'edit' && dept) {
      form.setFieldsValue({
        deptName: dept.deptName, parentId: dept.parentId || 0,
        phone: dept.phone || '', email: dept.email || '',
        sortOrder: dept.sortOrder ?? 0, status: dept.status !== 0,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ parentId: parentId, status: true, sortOrder: 0 });
    }
    setDrawerVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        deptName: values.deptName, parentId: values.parentId ?? currentParentId ?? 0,
        phone: values.phone || null, email: values.email || null,
        sortOrder: Number(values.sortOrder || 0), status: values.status ? 1 : 0,
      };
      setSubmitLoading(true);
      if (drawerMode === 'create') {
        await request.post('/system/dept', payload);
        message.success('新增成功');
      } else if (editingId) {
        await request.put('/system/dept', { ...payload, id: editingId });
        message.success('更新成功');
      }
      setDrawerVisible(false);
      form.resetFields();
      loadDeptTree();
    } catch (e) {
      if (e?.errorFields) return;
      message.error(e?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = () => {
    if (!selectedDept) return;
    Modal.confirm({
      title: '确认删除', content: `确定要删除「${selectedDept.deptName}」吗？如有子部门将一并删除。`,
      okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
      onOk: async () => {
        try {
          await request.delete(`/system/dept/${selectedDept.id}`);
          message.success('删除成功');
          setSelectedDept(null); setSelectedKeys([]);
          loadDeptTree();
        } catch (e) { message.error(e?.message || '删除失败'); }
      },
    });
  };

  const findDeptName = (nodes, id) => {
    for (const n of nodes) {
      if (n.id === id) return n.deptName;
      if (n.children?.length > 0) { const f = findDeptName(n.children, id); if (f) return f; }
    }
    return null;
  };

  return (
    <div className="permission-list-page">
      <div className="permission-toolbar">
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create', 0)}>新增顶级部门</Button>
          <Button icon={<PlusOutlined />} disabled={!selectedDept} onClick={() => openDrawer('create', selectedDept?.id || 0)}>新增子部门</Button>
          <Button icon={<EditOutlined />} disabled={!selectedDept} onClick={() => openDrawer('edit', selectedDept?.parentId || 0, selectedDept)}>编辑</Button>
          <Button danger icon={<DeleteOutlined />} disabled={!selectedDept} onClick={handleDelete}>删除</Button>
        </Space>
      </div>

      <div className="permission-content">
        <div className="tree-section">
          <div className="section-title">组织架构</div>
          {loading ? <Skeleton active paragraph={{ rows: 8 }} style={{ padding: 12 }} /> :
            deptTree.length > 0 ? (
              <Tree showLine={{ showLeafIcon: false }} expandedKeys={expandedKeys} selectedKeys={selectedKeys}
                onSelect={handleSelect} onExpand={setExpandedKeys} className="permission-tree">
                {renderTreeNodes(deptTree)}
              </Tree>
            ) : <Empty description="暂无部门数据" />
          }
        </div>

        <div className="detail-section">
          <div className="section-title">部门详情</div>
          {selectedDept ? (
            <Descriptions column={1} bordered className="permission-detail-descriptions">
              <Descriptions.Item label="部门名称">{selectedDept.deptName || '-'}</Descriptions.Item>
              <Descriptions.Item label="上级部门">
                {selectedDept.parentId ? `${findDeptName(deptTree, selectedDept.parentId) || '未知'} (ID: ${selectedDept.parentId})` : '无（顶级部门）'}
              </Descriptions.Item>
              <Descriptions.Item label="联系电话">
                {selectedDept.phone ? <><PhoneOutlined style={{ marginRight: 6 }} />{selectedDept.phone}</> : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="邮箱">
                {selectedDept.email ? <><MailOutlined style={{ marginRight: 6 }} />{selectedDept.email}</> : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="排序">{selectedDept.sortOrder ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedDept.status === 1 ? 'success' : 'error'}>{selectedDept.status === 1 ? '启用' : '禁用'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{selectedDept.createTime ? formatDateTime(selectedDept.createTime) : '-'}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{selectedDept.updateTime ? formatDateTime(selectedDept.updateTime) : '-'}</Descriptions.Item>
            </Descriptions>
          ) : <Empty description="请选择一个部门查看详情" />}
        </div>
      </div>

      <Drawer title={drawerMode === 'edit' ? '编辑部门' : '新增部门'} placement="right" width={480}
        open={drawerVisible} onClose={() => { setDrawerVisible(false); form.resetFields(); }}
        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type="primary" loading={submitLoading} onClick={handleSubmit}>保存</Button></Space>}>
        <Form form={form} layout="vertical">
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入' }]}>
            <Input placeholder="如 技术部" />
          </Form.Item>
          <Form.Item name="parentId" label="上级部门 ID">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="phone" label="联系电话"><Input placeholder="010-12345678" prefix={<PhoneOutlined />} /></Form.Item></Col>
            <Col span={12}><Form.Item name="email" label="邮箱"><Input placeholder="dept@company.com" prefix={<MailOutlined />} /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item></Col>
            <Col span={8}><Form.Item name="status" label="状态" valuePropName="checked"><Switch checkedChildren="启用" unCheckedChildren="禁用" /></Form.Item></Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
