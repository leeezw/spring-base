import { useState, useCallback, useEffect, useMemo } from 'react';
import { Space, Tag, Tree, Empty, Skeleton, Button, Modal, Form, Input, Select, Switch, InputNumber, message, Drawer, Row, Col, TreeSelect, Badge, Tooltip } from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, IdcardOutlined, TeamOutlined,
  ApartmentOutlined, UserOutlined, SearchOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { usePermission } from '../hooks/usePermission.jsx';
import { useDict } from '../hooks/useDict.jsx';
import './PostList.css';

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
  const [searchText, setSearchText] = useState('');

  const loadDeptTree = useCallback(async () => {
    try {
      const res = await request.get('/system/dept/tree');
      if (res.code === 200) setDeptTree(Array.isArray(res.data) ? res.data : []);
    } catch (e) { /* ignore */ }
  }, []);

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

  // 总岗位数
  const totalCount = useMemo(() => {
    return postTree.reduce((sum, dept) => sum + (dept.children || []).length, 0);
  }, [postTree]);

  // 搜索过滤
  const filteredTree = useMemo(() => {
    if (!searchText.trim()) return postTree;
    const keyword = searchText.toLowerCase();
    return postTree.map(dept => {
      const filteredChildren = (dept.children || []).filter(p =>
        p.title.toLowerCase().includes(keyword) ||
        (p.postCode || '').toLowerCase().includes(keyword)
      );
      if (filteredChildren.length > 0 || dept.title.toLowerCase().includes(keyword)) {
        return { ...dept, children: filteredChildren };
      }
      return null;
    }).filter(Boolean);
  }, [postTree, searchText]);

  // 树节点渲染
  const treeData = useMemo(() => {
    return filteredTree.map(dept => ({
      key: dept.key,
      title: (
        <div className="post-dept-node">
          <TeamOutlined />
          <span>{dept.title}</span>
          <span className="dept-count">{(dept.children || []).length} 个岗位</span>
        </div>
      ),
      selectable: false,
      children: (dept.children || []).map(post => ({
        key: post.key,
        title: (
          <div className="post-item-node">
            <IdcardOutlined />
            <span className="post-name">{post.title}</span>
            <div className="post-tags">
              <Tag color={getCategoryColor(post.postCategory)}>
                {getCategoryLabel(post.postCategory)}
              </Tag>
              {post.status !== 1 && <Tag color="default">停用</Tag>}
              {post.userCount > 0 && (
                <Badge count={post.userCount} size="small"
                  className="post-user-badge"
                  style={{ backgroundColor: '#3f8cff' }}
                />
              )}
            </div>
          </div>
        ),
        postData: post,
      })),
    }));
  }, [filteredTree, getCategoryColor, getCategoryLabel]);

  const handleSelect = useCallback((selectedKeys) => {
    if (selectedKeys.length === 0) return;
    const key = selectedKeys[0];
    for (const dept of postTree) {
      for (const post of (dept.children || [])) {
        if (post.key === key) {
          setSelectedPost({ ...post, deptTitle: dept.title, deptType: dept.type });
          return;
        }
      }
    }
  }, [postTree]);

  const openDrawer = useCallback((mode, post = null) => {
    setDrawerMode(mode);
    setDrawerVisible(true);
    if (mode === 'edit' && post) {
      setEditingId(post.key);
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

  const handleSubmit = useCallback(async (values) => {
    setSubmitLoading(true);
    try {
      const payload = { ...values, status: values.status ? 1 : 0, deptId: values.deptId || null };
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

  const buildDeptTreeSelect = (nodes) => {
    if (!nodes) return [];
    return nodes.map(n => ({
      value: n.id, title: n.deptName,
      children: n.children?.length > 0 ? buildDeptTreeSelect(n.children) : undefined,
    }));
  };

  return (
    <div className="post-page">
      {/* 工具栏 */}
      <div className="post-toolbar">
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

      {/* 主内容区 */}
      <div className="post-content">
        {/* 左侧树 */}
        <div className="post-tree-panel">
          <div className="post-tree-header">
            <h3>
              <ApartmentOutlined />
              组织岗位
              <Tag color="processing" style={{ marginLeft: 'auto', fontSize: 12, fontWeight: 400 }}>
                共 {totalCount} 个岗位
              </Tag>
            </h3>
            <div className="tree-subtitle">按部门分组展示所有岗位</div>
          </div>

          <div className="post-tree-search">
            <Input
              placeholder="搜索岗位名称 / 编码"
              prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
              allowClear
              value={searchText}
              onChange={e => setSearchText(e.target.value)}
              style={{ borderRadius: 8 }}
            />
          </div>

          <div className="post-tree-body">
            {loading ? (
              <Skeleton active paragraph={{ rows: 10 }} style={{ padding: '8px 12px' }} />
            ) : treeData.length > 0 ? (
              <Tree
                treeData={treeData}
                expandedKeys={expandedKeys}
                onExpand={setExpandedKeys}
                selectedKeys={selectedPost ? [selectedPost.key] : []}
                onSelect={handleSelect}
                blockNode
              />
            ) : (
              <Empty description={searchText ? '无匹配结果' : '暂无岗位数据'} style={{ marginTop: 60 }} />
            )}
          </div>
        </div>

        {/* 右侧详情 */}
        <div className="post-detail-panel">
          {selectedPost ? (
            <>
              <div className="post-detail-header">
                <div className="post-detail-icon">
                  <IdcardOutlined />
                </div>
                <div className="post-detail-title-area">
                  <h3>{selectedPost.title}</h3>
                  <div className="post-code">{selectedPost.postCode}</div>
                </div>
                <div className="post-detail-tags">
                  <Tag color={getCategoryColor(selectedPost.postCategory)}>
                    {getCategoryLabel(selectedPost.postCategory)}
                  </Tag>
                  <Tag color={selectedPost.status === 1 ? 'success' : 'default'}>
                    {selectedPost.status === 1 ? '启用' : '停用'}
                  </Tag>
                </div>
              </div>

              <div className="post-detail-body">
                {/* 统计卡片 */}
                <div className="post-stats">
                  <div className="post-stat-card">
                    <div className="stat-icon"><UserOutlined /></div>
                    <div className="stat-text">
                      <span className="stat-number">{selectedPost.userCount || 0}</span>
                      <span className="stat-label">在岗人数</span>
                    </div>
                  </div>
                  <div className="post-stat-card warning">
                    <div className="stat-icon"><ApartmentOutlined /></div>
                    <div className="stat-text">
                      <span className="stat-number">{selectedPost.deptType === 'global' ? '全局' : '部门'}</span>
                      <span className="stat-label">岗位归属</span>
                    </div>
                  </div>
                </div>

                {/* 信息网格 */}
                <div className="post-info-grid" style={{ marginTop: 24 }}>
                  <div className="post-info-item">
                    <span className="post-info-label">岗位编码</span>
                    <span className="post-info-value"><code>{selectedPost.postCode}</code></span>
                  </div>
                  <div className="post-info-item">
                    <span className="post-info-label">岗位类别</span>
                    <span className="post-info-value">
                      <Tag color={getCategoryColor(selectedPost.postCategory)}>
                        {getCategoryLabel(selectedPost.postCategory)}
                      </Tag>
                    </span>
                  </div>
                  <div className="post-info-item">
                    <span className="post-info-label">所属部门</span>
                    <span className="post-info-value">
                      {selectedPost.deptType === 'global' ? '全局岗位（不限部门）' : selectedPost.deptTitle}
                    </span>
                  </div>
                  <div className="post-info-item">
                    <span className="post-info-label">状态</span>
                    <span className="post-info-value">
                      <Tag color={selectedPost.status === 1 ? 'success' : 'default'}>
                        {selectedPost.status === 1 ? '启用' : '停用'}
                      </Tag>
                    </span>
                  </div>
                  {selectedPost.description && (
                    <div className="post-info-item full-width">
                      <span className="post-info-label">岗位描述</span>
                      <span className="post-info-value">{selectedPost.description}</span>
                    </div>
                  )}
                </div>
              </div>
            </>
          ) : (
            <div className="post-detail-empty">
              <IdcardOutlined />
              <p>请在左侧选择一个岗位查看详情</p>
            </div>
          )}
        </div>
      </div>

      {/* 新增/编辑抽屉 */}
      <Drawer
        title={drawerMode === 'edit' ? '编辑岗位' : '新增岗位'}
        placement="right"
        width={520}
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
                  label: <span><Tag color={i.itemColor} style={{ marginRight: 4 }}>{i.itemLabel}</Tag></span>,
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
