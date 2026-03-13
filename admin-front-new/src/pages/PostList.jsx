import { useState, useCallback, useEffect, useMemo } from 'react';
import { Space, Tag, Tree, Descriptions, Empty, Skeleton, Button, Modal, Form, Input, Select, Switch, InputNumber, message, Drawer, Divider, Row, Col, TreeSelect, Badge } from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, IdcardOutlined, TeamOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import { usePermission } from '../hooks/usePermission.jsx';
import { useDict } from '../hooks/useDict.jsx';
import './PermissionList.css';

export default function PostList() {
  const { hasPermission } = usePermission();
  const { items: categoryItems, getLabel: getCategoryLabel, getColor: getCategoryColor } = useDict('post_category');
  const [loading, setLoading] = useState(false);
  const [postTree, setPostTree] = useState([]);
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [selectedPost, setSelectedPost] = useState(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [drawerMode, setDrawerMode] = useState('create');
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [deptTree, setDeptTree] = useState([]);

  // 加载部门树（给表单用）
  const loadDeptTree = useCallback(async () => {
    try {
      const res = await request.get('/system/dept/tree');
      if (res.code === 200) setDeptTree(Array.isArray(res.data) ? res.data : []);
    } catch (e) { /* ignore */ }
  }, []);

  // 加载部门-岗位树
  const loadPostTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/system/post/dept-tree');
      if (res.code === 200 && res.data) {
        setPostTree(res.data);
        if (expandedKeys.length === 0) {
          setExpandedKeys(res.data.map(d => d.key));
        }
      }
    } catch (e) {
      message.error('加载岗位数据失败');
    } finally {
      setLoading(false);
    }
  }, [expandedKeys.length]);

  useEffect(() => { loadPostTree(); loadDeptTree(); }, [loadPostTree, loadDeptTree]);

  // 树节点渲染
  const treeData = useMemo(() => {
    return postTree.map(dept => ({
      key: dept.key,
      title: (
        <span className="tree-node-content">
          <TeamOutlined style={{ marginRight: 6, color: '#8c8c8c' }} />
          <strong>{dept.title}</strong>
        </span>
      ),
      selectable: false,
      children: (dept.children || []).map(post => ({
        key: post.key,
        title: (
          <span className="tree-node-content">
            <IdcardOutlined style={{ marginRight: 6 }} />
            <span>{post.title}</span>
            {' '}
            <Tag color={getCategoryColor(post.postCategory)} style={{ fontSize: 11, lineHeight: '18px', padding: '0 4px' }}>
              {getCategoryLabel(post.postCategory)}
            </Tag>
            <Tag color={post.status === 1 ? 'success' : 'default'} style={{ fontSize: 11, lineHeight: '18px', padding: '0 4px' }}>
              {post.status === 1 ? '启用' : '停用'}
            </Tag>
            {post.userCount > 0 && (
              <Badge count={post.userCount} size="small" style={{ backgroundColor: '#3f8cff', marginLeft: 4 }} />
            )}
          </span>
        ),
        postData: post,
      })),
    }));
  }, [postTree]);

  // 选中岗位 — 需要从原始数据找
  const handleSelect = useCallback((selectedKeys) => {
    if (selectedKeys.length === 0) return;
    const key = selectedKeys[0];
    // 从postTree找
    for (const dept of postTree) {
      for (const post of (dept.children || [])) {
        if (post.key === key) {
          setSelectedPost({ ...post, deptTitle: dept.title, deptType: dept.type });
          return;
        }
      }
    }
  }, [postTree]);

  // 打开抽屉
  const openDrawer = useCallback((mode, post = null) => {
    setDrawerMode(mode);
    setDrawerVisible(true);
    if (mode === 'edit' && post) {
      setEditingId(post.key);
      // 需要从API获取完整岗位信息
      request.get('/system/post/list').then(res => {
        if (res.code === 200) {
          const fullPost = res.data.find(p => p.id.toString() === post.key);
          if (fullPost) {
            form.setFieldsValue({
              postCode: fullPost.postCode,
              postName: fullPost.postName,
              postCategory: fullPost.postCategory,
              deptId: fullPost.deptId || null,
              sortOrder: fullPost.sortOrder || 0,
              status: fullPost.status === 1,
              description: fullPost.description || '',
            });
          }
        }
      });
    } else {
      setEditingId(null);
      form.setFieldsValue({ postCode: '', postName: '', postCategory: 3, deptId: null, sortOrder: 0, status: true, description: '' });
    }
  }, [form]);

  // 提交
  const handleSubmit = useCallback(async (values) => {
    setSubmitLoading(true);
    try {
      const payload = {
        ...values,
        status: values.status ? 1 : 0,
        deptId: values.deptId || null,
      };
      if (drawerMode === 'edit' && editingId) {
        payload.id = parseInt(editingId);
        const res = await request.put('/system/post', payload);
        if (res.code === 200) { message.success('编辑成功'); } else { message.error(res.message); return; }
      } else {
        const res = await request.post('/system/post', payload);
        if (res.code === 200) { message.success('新增成功'); } else { message.error(res.message); return; }
      }
      setDrawerVisible(false);
      loadPostTree();
    } catch (e) {
      message.error(e?.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  }, [drawerMode, editingId, loadPostTree]);

  // 删除
  const handleDelete = useCallback(() => {
    if (!selectedPost) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除「${selectedPost.title}」吗？关联的用户岗位也会被清除。`,
      okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
      onOk: async () => {
        try {
          const res = await request.delete(`/system/post/${selectedPost.key}`);
          if (res.code === 200) { message.success('删除成功'); setSelectedPost(null); loadPostTree(); }
          else message.error(res.message);
        } catch (e) { message.error(e?.message || '删除失败'); }
      }
    });
  }, [selectedPost, loadPostTree]);

  // 部门树选择数据
  const buildDeptTreeSelect = (nodes) => {
    if (!nodes) return [];
    return nodes.map(n => ({
      value: n.id, title: n.deptName,
      children: n.children?.length > 0 ? buildDeptTreeSelect(n.children) : undefined,
    }));
  };

  return (
    <div className="permission-page">
      <div className="permission-layout">
        {/* 左侧岗位树 */}
        <div className="permission-tree-panel">
          <div className="permission-toolbar">
            <Space>
              {hasPermission('system:post:add') && (
                <Button type="primary" icon={<PlusOutlined />} onClick={() => openDrawer('create')}>新增岗位</Button>
              )}
              {hasPermission('system:post:edit') && (
                <Button icon={<EditOutlined />} disabled={!selectedPost} onClick={() => openDrawer('edit', selectedPost)}>编辑</Button>
              )}
              {hasPermission('system:post:delete') && (
                <Button danger icon={<DeleteOutlined />} disabled={!selectedPost} onClick={handleDelete}>删除</Button>
              )}
            </Space>
          </div>

          <div className="tree-title">按部门分组</div>

          {loading ? <Skeleton active paragraph={{ rows: 8 }} style={{ padding: 16 }} /> : (
            treeData.length > 0 ? (
              <Tree
                treeData={treeData}
                expandedKeys={expandedKeys}
                onExpand={setExpandedKeys}
                selectedKeys={selectedPost ? [selectedPost.key] : []}
                onSelect={handleSelect}
                blockNode
                style={{ padding: '8px 12px' }}
              />
            ) : (
              <Empty description="暂无岗位数据" style={{ marginTop: 60 }} />
            )
          )}
        </div>

        {/* 右侧详情 */}
        <div className="permission-detail-panel">
          {selectedPost ? (
            <div className="detail-content">
              <div className="detail-header">
                <IdcardOutlined style={{ fontSize: 20, color: '#3f8cff' }} />
                <span className="detail-title">{selectedPost.title}</span>
                <Tag color={getCategoryColor(selectedPost.postCategory)}>
                  {getCategoryLabel(selectedPost.postCategory)}
                </Tag>
                <Tag color={selectedPost.status === 1 ? 'success' : 'default'}>
                  {selectedPost.status === 1 ? '启用' : '停用'}
                </Tag>
              </div>
              <Divider style={{ margin: '12px 0' }} />
              <Descriptions column={1} size="small" labelStyle={{ width: 100, color: '#8c8c8c' }}>
                <Descriptions.Item label="岗位编码">
                  <code style={{ background: '#f5f5f5', padding: '2px 8px', borderRadius: 4 }}>{selectedPost.postCode}</code>
                </Descriptions.Item>
                <Descriptions.Item label="所属部门">
                  {selectedPost.deptType === 'global' ? '全局岗位（不限部门）' : selectedPost.deptTitle}
                </Descriptions.Item>
                <Descriptions.Item label="岗位类别">
                  <Tag color={getCategoryColor(selectedPost.postCategory)}>
                    {getCategoryLabel(selectedPost.postCategory)}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="在岗人数">
                  <Badge count={selectedPost.userCount || 0} showZero size="small" style={{ backgroundColor: selectedPost.userCount > 0 ? '#3f8cff' : '#d9d9d9' }} />
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={selectedPost.status === 1 ? 'success' : 'default'}>
                    {selectedPost.status === 1 ? '启用' : '停用'}
                  </Tag>
                </Descriptions.Item>
              </Descriptions>
            </div>
          ) : (
            <Empty description="请在左侧选择一个岗位" style={{ marginTop: 120 }} />
          )}
        </div>
      </div>

      {/* 新增/编辑抽屉 */}
      <Drawer
        title={drawerMode === 'edit' ? '编辑岗位' : '新增岗位'}
        placement="right"
        width={480}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        destroyOnClose
        extra={
          <Space>
            <Button onClick={() => setDrawerVisible(false)}>取消</Button>
            <Button type="primary" onClick={() => form.submit()} loading={submitLoading}>保存</Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit} initialValues={{ postCategory: 3, sortOrder: 0, status: true }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="postName" label="岗位名称" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="如 Java开发工程师" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="postCode" label="岗位编码" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="如 java_dev" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="postCategory" label="岗位类别" rules={[{ required: true }]}>
                <Select options={categoryItems.map(i => ({
                  value: parseInt(i.itemValue),
                  label: <span><Tag color={i.itemColor} style={{ marginRight: 4 }}>{i.itemLabel}</Tag>{i.description || ''}</span>,
                }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deptId" label="所属部门">
                <TreeSelect
                  treeData={[{ value: null, title: '全局岗位（不限部门）' }, ...buildDeptTreeSelect(deptTree)]}
                  placeholder="不选则为全局岗位"
                  treeDefaultExpandAll
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="岗位描述">
            <Input.TextArea rows={3} placeholder="描述岗位职责" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="sortOrder" label="排序">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
