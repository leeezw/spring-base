import { useState, useRef, useCallback, useEffect } from 'react';
import { Button, Space, Tag, Modal, Form, message, Input, Select, Checkbox, Dropdown, Drawer } from 'antd';
import {
 
  UserOutlined, 
  CheckCircleOutlined, 
  StopOutlined, 
  PlusOutlined,
  EditOutlined,
  SafetyOutlined,
  PoweroffOutlined,
  DeleteOutlined,
  MoreOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { ProFormText, ProFormSelect } from '@ant-design/pro-components';
import request from '../api/index.js';
import UserForm from '../components/UserForm.jsx';
import RoleSelectModal from '../components/RoleSelectModal.jsx';
import TableSearchForm from '../components/TableSearchForm.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';
import { usePermission } from '../hooks/usePermission.jsx';
import './UserList.css';

const { Search } = Input;

export default function UserList() {
  const { hasPermission } = usePermission();
  const actionRef = useRef();
  const [stats, setStats] = useState({
    total: 0,
    enabled: 0,
    disabled: 0,
    today: 0
  });
  const [form] = Form.useForm();
  const [filterForm] = Form.useForm();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [filterParams, setFilterParams] = useState({ status: 'all' });
  const [roleModalVisible, setRoleModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const debounceTimerRef = useRef(null);

  // 防抖处理筛选
  const handleFilterChange = useCallback(() => {
    // 清除之前的定时器
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    // 设置新的定时器，500ms 后执行搜索
    debounceTimerRef.current = setTimeout(() => {
      filterForm.submit();
    }, 500);
  }, [filterForm]);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // 请求函数 - 适配 ProTableV2
  const fetchUsers = async (params) => {
    try {
      // 处理日期时间范围参数
      const requestParams = { ...params };
      
      // 如果存在日期范围，提取开始时间和结束时间
      if (params.dateRange) {
        requestParams.startTime = params.startTime;
        requestParams.endTime = params.endTime;
        // 移除 dateRange 对象，避免发送到后端
        delete requestParams.dateRange;
      }
      
      const res = await request.get('/system/user/page', { params: requestParams });
      // 适配我们后端的分页格式：{ code, data: { records, total, current, size } }
      if (res.code === 200 && res.data) {
        const pageData = res.data;
        // 更新统计数据
        setStats({
          total: pageData.total || 0,
          enabled: 0,
          disabled: 0,
          today: 0
        });
        
        // 返回 ProTableV2 期望的格式
        return {
          code: 200,
          data: { list: pageData.records || [], total: pageData.total || 0 }
        };
      }
      return res;
    } catch (error) {
      console.error('fetchUsers error:', error);
      return {
        code: 500,
        data: { list: [], total: 0 }
      };
    }
  };

  // 处理数据变化（仅用于通知，不触发刷新，避免无限循环）
  const handleDataChange = (data, total) => {
    // 统计数据已从后端获取，这里不需要再计算
  };

  const handleRefresh = () => {
    actionRef.current?.reload();
  };

  const handleBatchStatus = (status) => {
    Modal.confirm({
      title: status === 1 ? '批量启用' : '批量禁用',
      content: `确定将选中的 ${selectedRowKeys.length} 个用户${status === 1 ? '启用' : '禁用'}？`,
      onOk: async () => {
        const res = await request.put('/system/user/batch-status', { ids: selectedRowKeys, status });
        if (res.code === 200) { message.success('操作成功'); setSelectedRowKeys([]); actionRef.current?.reload(); }
        else message.error(res.message);
      }
    });
  };

  const handleBatchDelete = () => {
    Modal.confirm({
      title: '批量删除',
      content: `确定删除选中的 ${selectedRowKeys.length} 个用户？此操作不可恢复！`,
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete('/system/user/batch', { data: selectedRowKeys });
        if (res.code === 200) { message.success('删除成功'); setSelectedRowKeys([]); actionRef.current?.reload(); }
        else message.error(res.message);
      }
    });
  };

  const handleExportUsers = () => {
    const token = localStorage.getItem('uc_token');
    window.open(`/api/system/export/users?token=${token}`, '_blank');
  };

  const handleAddUser = () => {
    setEditingUser(null);
    form.resetFields();
    // 确保表单字段完全清空
    form.setFieldsValue({
      username: undefined,
      password: undefined,
      nickname: undefined,
      email: undefined,
      status: 1,
    });
    setModalVisible(true);
  };

  const handleEditUser = (record) => {
    setEditingUser(record);
    form.setFieldsValue({
      username: record.username,
      nickname: record.nickname,
      email: record.email,
      // 编辑时不设置状态，状态通过独立的状态按钮修改
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values) => {
    setSubmitLoading(true);
    try {
      const { roleIds, postIds, ...userData } = values;
      
      // 编辑模式下，如果密码为空则移除密码字段
      if (editingUser && !userData.password) {
        delete userData.password;
      }

      let userId;
      if (editingUser) {
        // 编辑用户
        const res = await request.put('/system/user', { ...userData, id: editingUser.id });
        if (res.code !== 200) {
          message.error(res.message || '更新失败');
          return;
        }
        userId = editingUser.id;
      } else {
        // 新增用户
        const res = await request.post('/system/user', userData);
        if (res.code !== 200) {
          message.error(res.message || '创建失败');
          return;
        }
        // 新增成功后需要拿到用户ID来分配角色
        userId = res.data?.id;
      }

      // 分配角色（如果有选择角色且有userId）
      if (userId && roleIds && roleIds.length > 0) {
        try {
          await request.post(`/system/relation/user/${userId}/roles`, { ids: roleIds });
        } catch (e) {
          console.warn('角色分配失败:', e);
        }
      }

      // 分配岗位
      if (userId && postIds && postIds.length > 0) {
        try {
          await request.post(`/system/relation/user/${userId}/posts`, { ids: postIds });
        } catch (e) {
          console.warn('岗位分配失败:', e);
        }
      }
      
      message.success(editingUser ? '用户更新成功' : '用户创建成功');
      setModalVisible(false);
      form.resetFields();
      setEditingUser(null);
      handleRefresh();
    } catch (error) {
      message.error(error.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleCancel = () => {
    setModalVisible(false);
    form.resetFields();
    // 清空表单字段
    form.setFieldsValue({
      username: undefined,
      password: undefined,
      nickname: undefined,
      email: undefined,
      status: 1,
    });
    setEditingUser(null);
  };

  // 修改用户状态
  const handleChangeStatus = async (record) => {
    const newStatus = record.status === 1 ? 0 : 1;
    const statusText = newStatus === 1 ? '启用' : '禁用';
    
    Modal.confirm({
      title: `确认${statusText}用户`,
      content: `确定要${statusText}用户 "${record.nickname || record.username}" 吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 根据 OpenAPI 规范：/api/users/status PUT
          // Request: { id, status }
          const res = await request.put('/system/user', {
            id: record.id,
            status: newStatus
          });
          
          if (res.code === 200) {
            message.success(`用户已${statusText}`);
            handleRefresh();
          } else {
            message.error(res.message || `${statusText}失败`);
          }
        } catch (error) {
          message.error(error.message || `${statusText}失败`);
        }
      }
    });
  };

  // 授予角色
  const handleAssignRoles = (record) => {
    setSelectedUser(record);
    setRoleModalVisible(true);
  };

  // 删除用户
  const handleDeleteUser = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除用户「${record.nickname || record.username}」吗？`,
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          const res = await request.delete(`/system/user/${record.id}`);
          if (res.code === 200) {
            message.success('删除成功');
            handleRefresh();
          } else {
            message.error(res.message || '删除失败');
          }
        } catch (e) {
          message.error('操作失败');
        }
      },
    });
  };

  // 角色授予成功回调
  const handleRoleAssignSuccess = () => {
    setRoleModalVisible(false);
    setSelectedUser(null);
    handleRefresh();
  };

  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys) => {
      setSelectedRowKeys(keys);
    },
    getCheckboxProps: (record) => ({
      disabled: false,
    }),
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      ellipsis: true,
      fixed: 'left',
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
      width: 100,
      render: (text) => text || '-',
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      width: 180,
      render: (_, record) => {
        const roles = record.roles || [];
        if (roles.length === 0) return <Tag>未分配</Tag>;
        return (
          <Space size={[0, 4]} wrap>
            {roles.map(r => (
              <Tag key={r.id} color="blue">{r.roleName}</Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '岗位',
      dataIndex: 'posts',
      key: 'posts',
      width: 150,
      render: (_, record) => {
        const posts = record.posts || [];
        if (posts.length === 0) return <Tag>未分配</Tag>;
        return (
          <Space size={[0, 4]} wrap>
            {posts.map(p => (
              <Tag key={p.id} color="cyan">{p.postName}</Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '状态',
      width: 80,
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          {hasPermission('system:user:edit') && (
            <Button 
              type="text" 
              icon={<EditOutlined />} 
              size="small"
              title="编辑"
              onClick={() => handleEditUser(record)}
            />
          )}
          {hasPermission('system:user:edit') && (
            <Button 
              type="text" 
              icon={<PoweroffOutlined />}
              size="small"
              title={record.status === 1 ? '禁用' : '启用'}
              danger={record.status === 1}
              onClick={() => handleChangeStatus(record)}
            />
          )}
          {hasPermission('system:user:edit') && (
            <Button 
              type="text" 
              icon={<SafetyOutlined />} 
              size="small"
              title="授予角色"
              onClick={() => handleAssignRoles(record)}
            />
          )}
          {hasPermission('system:user:delete') && (
            <Button 
              type="text" 
              icon={<DeleteOutlined />} 
              size="small"
              title="删除"
              danger
              onClick={() => handleDeleteUser(record)}
            />
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="user-list-page">
      {/* 搜索表单区域 */}
      <div className="search-section">
        {/* 内联统计指标 */}
        <div className="inline-stats">
            <span className="inline-stat">
              <UserOutlined style={{ color: '#3f8cff' }} />
              <span className="inline-stat-label">总数</span>
              <span className="inline-stat-value">{stats.total}</span>
            </span>
            <span className="inline-stat">
              <CheckCircleOutlined style={{ color: '#22c55e' }} />
              <span className="inline-stat-label">启用</span>
              <span className="inline-stat-value">{stats.enabled}</span>
            </span>
            <span className="inline-stat">
              <StopOutlined style={{ color: '#fb923c' }} />
              <span className="inline-stat-label">禁用</span>
              <span className="inline-stat-value">{stats.disabled}</span>
            </span>
            <span className="inline-stat">
              <PlusOutlined style={{ color: '#a855f7' }} />
              <span className="inline-stat-label">今日</span>
              <span className="inline-stat-value">{stats.today}</span>
            </span>
        </div>
        <TableSearchForm
          form={filterForm}
          initialValues={{ status: 'all' }}
          onFinish={async (values) => {
            // 当筛选条件变化时，更新筛选参数并触发表格刷新
            const newFilterParams = { ...values };
            // 处理 status 参数（如果存在且是 'all'，则移除）
            if (newFilterParams.status === 'all') {
              delete newFilterParams.status;
            }
            setFilterParams(newFilterParams);
            // 触发表格刷新，ProTable 会将 filterParams 合并到 requestParams 中
            actionRef.current?.reload();
          }}
          onKeywordChange={handleFilterChange}
          config={{
            showKeyword: true,
            keywordPlaceholder: '搜索用户名、昵称、邮箱或手机号',
            showStatus: true,
            statusOptions: [
              { label: '全部', value: 'all' },
              { label: '启用', value: 1 },
              { label: '禁用', value: 0 },
            ],
            showDateRange: true,
            dateRangePlaceholder: ['创建开始时间', '创建结束时间'],
            dateRangeStartName: 'startTime',
            dateRangeEndName: 'endTime',
          }}
        />
      </div>

      {/* 批量操作区域 */}
      {/* 数据表格 */}
      <ProTableV2
        actionRef={actionRef}
        headerTitle="用户列表"
        columns={columns}
        request={fetchUsers}
        rowKey="id"
        onDataChange={handleDataChange}
        search={false}
        scroll={{ x: 900 }}
        defaultSort={{
          createTime: 'desc',
        }}
        rowSelection={rowSelection}
        toolbar={{
          actions: [
            selectedRowKeys.length > 0 && hasPermission('system:user:edit') && (
              <Button key="batch-enable" onClick={() => handleBatchStatus(1)}>
                批量启用 ({selectedRowKeys.length})
              </Button>
            ),
            selectedRowKeys.length > 0 && hasPermission('system:user:edit') && (
              <Button key="batch-disable" danger onClick={() => handleBatchStatus(0)}>
                批量禁用 ({selectedRowKeys.length})
              </Button>
            ),
            selectedRowKeys.length > 0 && hasPermission('system:user:delete') && (
              <Button key="batch-delete" danger type="primary" onClick={handleBatchDelete}>
                批量删除 ({selectedRowKeys.length})
              </Button>
            ),
            hasPermission('system:user:query') && (
              <Button key="export" icon={<DownloadOutlined />} onClick={handleExportUsers}>
                导出Excel
              </Button>
            ),
            hasPermission('system:user:add') && (
              <Button 
                key="add" 
                type="primary" 
                icon={<PlusOutlined />} 
                onClick={handleAddUser}
              >
                新增用户
              </Button>
            ),
          ].filter(Boolean),
        }}
        params={filterParams}
      />

      {/* 新增/编辑用户抽屉 */}
      <Drawer
        title={editingUser ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onClose={handleCancel}
        width={480}
        destroyOnClose
        extra={
          <Space>
            <Button onClick={handleCancel}>取消</Button>
            <Button type="primary" onClick={() => form.submit()} loading={submitLoading}>
              {editingUser ? '更新' : '创建'}
            </Button>
          </Space>
        }
      >
        <UserForm
          key={editingUser ? `edit-${editingUser.id}` : 'add'}
          form={form}
          initialValues={editingUser}
          onFinish={handleSubmit}
        />
      </Drawer>

      {/* 授予角色弹窗 */}
      <RoleSelectModal
        visible={roleModalVisible}
        userId={selectedUser?.id}
        userName={selectedUser?.nickname || selectedUser?.username}
        currentRoleIds={selectedUser?.roleIds || []}
        onCancel={() => {
          setRoleModalVisible(false);
          setSelectedUser(null);
        }}
        onOk={handleRoleAssignSuccess}
      />
    </div>
  );
}
