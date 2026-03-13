import { useState, useCallback, useEffect } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Switch, InputNumber, message, Drawer, Divider, Row, Col, TreeSelect, Tabs, Table, Badge, Select } from 'antd';
import { 
  ApartmentOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PhoneOutlined,
  MailOutlined,
  IdcardOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import { usePermission } from '../hooks/usePermission.jsx';
import { useDict } from '../hooks/useDict.jsx';
import './PermissionList.css'; // 复用权限管理的样式

const { TreeNode } = Tree;

export default function DeptList() {
  const { hasPermission } = usePermission();
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

  // 岗位相关
  const [deptPosts, setDeptPosts] = useState([]);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postDrawerVisible, setPostDrawerVisible] = useState(false);
  const [editingPost, setEditingPost] = useState(null);
  const [postForm] = Form.useForm();
  const [postSubmitLoading, setPostSubmitLoading] = useState(false);
  const { items: categoryItems, getLabel: getCategoryLabel, getColor: getCategoryColor } = useDict('post_category');

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

  // 部门树转为TreeSelect格式
  const buildDeptTreeSelect = (nodes, excludeId = null) => {
    if (!nodes) return [];
    return nodes.filter(n => n.id !== excludeId).map(n => ({
      value: n.id, title: n.deptName,
      children: n.children?.length > 0 ? buildDeptTreeSelect(n.children, excludeId) : undefined,
    }));
  };

  const findDeptName = (nodes, id) => {
    for (const n of nodes) {
      if (n.id === id) return n.deptName;
      if (n.children?.length > 0) { const f = findDeptName(n.children, id); if (f) return f; }
    }
    return null;
  };

  // ============ 岗位管理 ============
  const loadDeptPosts = useCallback(async (deptId) => {
    setPostsLoading(true);
    try {
      const res = await request.get('/system/post/list', { params: { deptId, status: null } });
      if (res.code === 200) setDeptPosts(res.data || []);
    } catch (e) { /* ignore */ }
    finally { setPostsLoading(false); }
  }, []);

  // 选中部门时也加载岗位
  useEffect(() => {
    if (selectedDept?.id) loadDeptPosts(selectedDept.id);
    else setDeptPosts([]);
  }, [selectedDept?.id, loadDeptPosts]);

  const handleAddPost = () => {
    setEditingPost(null);
    postForm.setFieldsValue({ postName: '', postCode: '', postCategory: 3, sortOrder: 0, status: true, description: '' });
    setPostDrawerVisible(true);
  };

  const handleEditPost = (record) => {
    setEditingPost(record);
    postForm.setFieldsValue({ ...record, status: record.status === 1 });
    setPostDrawerVisible(true);
  };

  const handlePostSubmit = async (values) => {
    setPostSubmitLoading(true);
    try {
      const payload = { ...values, status: values.status ? 1 : 0, deptId: selectedDept.id };
      if (editingPost) {
        payload.id = editingPost.id;
        const res = await request.put('/system/post', payload);
        if (res.code === 200) message.success('更新成功');
        else { message.error(res.message); return; }
      } else {
        const res = await request.post('/system/post', payload);
        if (res.code === 200) message.success('新增成功');
        else { message.error(res.message); return; }
      }
      setPostDrawerVisible(false);
      loadDeptPosts(selectedDept.id);
    } catch (e) { message.error('操作失败'); }
    finally { setPostSubmitLoading(false); }
  };

  const handleDeletePost = (record) => {
    Modal.confirm({
      title: '确认删除', content: `删除「${record.postName}」？`,
      okText: '删除', okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/system/post/${record.id}`);
        if (res.code === 200) { message.success('删除成功'); loadDeptPosts(selectedDept.id); }
        else message.error(res.message);
      }
    });
  };

  const postColumns = [
    { title: '岗位名称', dataIndex: 'postName', key: 'postName', width: 140 },
    { title: '编码', dataIndex: 'postCode', key: 'postCode', width: 130,
      render: (v) => <code style={{ background: '#f5f5f5', padding: '2px 6px', borderRadius: 4, fontSize: 12 }}>{v}</code> },
    { title: '类别', dataIndex: 'postCategory', key: 'postCategory', width: 80,
      render: (v) => <Tag color={getCategoryColor(v)}>{getCategoryLabel(v)}</Tag> },
    { title: '在岗', dataIndex: 'userCount', key: 'userCount', width: 60, align: 'center',
      render: (v) => <Badge count={v || 0} showZero size="small" style={{ backgroundColor: v > 0 ? '#3f8cff' : '#d9d9d9' }} /> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 60,
      render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag> },
    { title: '操作', key: 'action', width: 80,
      render: (_, record) => (
        <Space size={4}>
          {hasPermission('system:post:edit') && <Button type="text" icon={<EditOutlined />} size="small" onClick={() => handleEditPost(record)} />}
          {hasPermission('system:post:delete') && <Button type="text" icon={<DeleteOutlined />} size="small" danger onClick={() => handleDeletePost(record)} />}
        </Space>
      ),
    },
  ];

  return (
    <div className="permission-list-page">
      <div className="permission-toolbar">
        <Space>
          {hasPermission('system:dept:add') && <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create', 0)}>新增顶级部门</Button>}
          {hasPermission('system:dept:add') && <Button icon={<PlusOutlined />} disabled={!selectedDept} onClick={() => openDrawer('create', selectedDept?.id || 0)}>新增子部门</Button>}
          {hasPermission('system:dept:edit') && <Button icon={<EditOutlined />} disabled={!selectedDept} onClick={() => openDrawer('edit', selectedDept?.parentId || 0, selectedDept)}>编辑</Button>}
          {hasPermission('system:dept:delete') && <Button danger icon={<DeleteOutlined />} disabled={!selectedDept} onClick={handleDelete}>删除</Button>}
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
          {selectedDept ? (
            <Tabs defaultActiveKey="info" items={[
              {
                key: 'info',
                label: <span><ApartmentOutlined /> 基本信息</span>,
                children: (
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
                ),
              },
              {
                key: 'posts',
                label: <span><IdcardOutlined /> 岗位管理 <Badge count={deptPosts.length} size="small" style={{ marginLeft: 4, backgroundColor: '#3f8cff' }} /></span>,
                children: (
                  <div>
                    <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ color: '#8c8c8c', fontSize: 13 }}>{selectedDept.deptName} 下的岗位</span>
                      {hasPermission('system:post:add') && (
                        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddPost}>新增岗位</Button>
                      )}
                    </div>
                    <Table
                      dataSource={deptPosts}
                      columns={postColumns}
                      rowKey="id"
                      loading={postsLoading}
                      pagination={false}
                      size="small"
                      locale={{ emptyText: <Empty description="暂无岗位，点击新增" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    />
                  </div>
                ),
              },
            ]} />
          ) : (
            <>
              <div className="section-title">部门详情</div>
              <Empty description="请选择一个部门查看详情" />
            </>
          )}
        </div>
      </div>

      <Drawer title={drawerMode === 'edit' ? '编辑部门' : '新增部门'} placement="right" width={480}
        open={drawerVisible} onClose={() => { setDrawerVisible(false); form.resetFields(); }}
        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type="primary" loading={submitLoading} onClick={handleSubmit}>保存</Button></Space>}>
        <Form form={form} layout="vertical">
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入' }]}>
            <Input placeholder="如 技术部" />
          </Form.Item>
          <Form.Item name="parentId" label="上级部门">
            <TreeSelect
              treeData={[{ value: 0, title: '无（顶级部门）' }, ...buildDeptTreeSelect(deptTree, editingId)]}
              placeholder="请选择上级部门"
              treeDefaultExpandAll
              allowClear
              style={{ width: '100%' }}
            />
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

      {/* 岗位新增/编辑抽屉 */}
      <Drawer
        title={editingPost ? '编辑岗位' : `新增岗位 — ${selectedDept?.deptName || ''}`}
        open={postDrawerVisible} onClose={() => setPostDrawerVisible(false)}
        width={420} destroyOnClose
        extra={<Space><Button onClick={() => setPostDrawerVisible(false)}>取消</Button><Button type="primary" onClick={() => postForm.submit()} loading={postSubmitLoading}>保存</Button></Space>}
      >
        <Form form={postForm} layout="vertical" onFinish={handlePostSubmit} initialValues={{ postCategory: 3, sortOrder: 0, status: true }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="postName" label="岗位名称" rules={[{ required: true }]}><Input placeholder="如 Java开发工程师" /></Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="postCode" label="岗位编码" rules={[{ required: true }]}><Input placeholder="如 java_dev" /></Form.Item>
            </Col>
          </Row>
          <Form.Item name="postCategory" label="岗位类别" rules={[{ required: true }]}>
            <Select options={categoryItems.map(i => ({
              value: parseInt(i.itemValue),
              label: <span><Tag color={i.itemColor} style={{ marginRight: 4 }}>{i.itemLabel}</Tag>{i.description || ''}</span>,
            }))} />
          </Form.Item>
          <Form.Item name="description" label="岗位描述"><Input.TextArea rows={2} /></Form.Item>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item></Col>
            <Col span={8}><Form.Item name="status" label="状态" valuePropName="checked"><Switch checkedChildren="启用" unCheckedChildren="停用" /></Form.Item></Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
