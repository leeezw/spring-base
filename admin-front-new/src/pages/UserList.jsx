import { useState, useRef, useCallback, useEffect } from 'react';
import { Card, Statistic, Button, Space, Tag, Modal, Form, message, Input, Select, Checkbox, Dropdown, Drawer } from 'antd';
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
} from '@ant-design/icons';
import { ProFormText, ProFormSelect } from '@ant-design/pro-components';
import request from '../api/index.js';
import UserForm from '../components/UserForm.jsx';
import RoleSelectModal from '../components/RoleSelectModal.jsx';
import TableSearchForm from '../components/TableSearchForm.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';
import { usePageToolbar } from '../components/AppLayout.jsx';
import './UserList.css';

const { Search } = Input;

export default function UserList() {
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
  const { statsVisible, setStatsVisible, setShowStatsToggle } = usePageToolbar(); // 从 AppLayout 获取统计控制
  const debounceTimerRef = useRef(null);
  
  // 组件挂载时显示统计切换按钮
  useEffect(() => {
    setShowStatsToggle(true);
    return () => {
      setShowStatsToggle(false);
    };
  }, [setShowStatsToggle]);

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
    console.log('handleDataChange', data, total);
    // 统计数据已从后端获取，这里不需要再计算
  };

  const handleRefresh = () => {
    actionRef.current?.reload();
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
      // 编辑模式下，如果密码为空则移除密码字段
      if (editingUser && !values.password) {
        delete values.password;
      }

      if (editingUser) {
        // 编辑用户
        const res = await request.put('/system/user', { ...values, id: editingUser.id });
        if (res.code === 200) {
          message.success('用户更新成功');
          setModalVisible(false);
          form.resetFields();
          setEditingUser(null);
          handleRefresh();
        } else {
          message.error(res.message || '更新失败');
        }
      } else {
        // 新增用户
        const res = await request.post('/system/user', values);
        if (res.code === 200) {
          message.success('用户创建成功');
          setModalVisible(false);
          form.resetFields();
          // 确保表单字段完全清空
          form.setFieldsValue({
            username: undefined,
            password: undefined,
            nickname: undefined,
            email: undefined,
            status: 1,
          });
          setEditingUser(null);
          handleRefresh();
        } else {
          message.error(res.message || '创建失败');
        }
      }
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

  // 角色授予成功回调
  const handleRoleAssignSuccess = () => {
    setRoleModalVisible(false);
    setSelectedUser(null);
    handleRefresh();
  };

  // 批量操作 - 批量删除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要删除的用户');
      return;
    }
    
    Modal.confirm({
      title: '确认批量删除',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个用户吗？此操作不可恢复。`,
      okText: '确认删除',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          const res = await request.delete('/system/user/batch', {
            data: { ids: selectedRowKeys }
          });
          if (res.code === 200) {
            message.success('批量删除成功');
            setSelectedRowKeys([]);
            handleRefresh();
          } else {
            message.error(res.message || '批量删除失败');
          }
        } catch (error) {
          message.error(error.message || '批量删除失败');
        }
      }
    });
  };

  // 批量操作 - 批量修改状态
  const handleBatchChangeStatus = (newStatus) => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要操作的用户');
      return;
    }
    
    const statusText = newStatus === 1 ? '启用' : '禁用';
    Modal.confirm({
      title: `确认批量${statusText}`,
      content: `确定要${statusText}选中的 ${selectedRowKeys.length} 个用户吗？`,
      okText: `确认${statusText}`,
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await request.put('/system/user/batch/status', {
            ids: selectedRowKeys,
            status: newStatus
          });
          if (res.code === 200) {
            message.success(`已成功${statusText} ${selectedRowKeys.length} 个用户`);
            setSelectedRowKeys([]);
            handleRefresh();
          } else {
            message.error(res.message || `批量${statusText}失败`);
          }
        } catch (error) {
          message.error(error.message || `批量${statusText}失败`);
        }
      }
    });
  };

  // 批量操作 - 批量授予角色
  const handleBatchAssignRoles = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要操作的用户');
      return;
    }
    // TODO: 实现批量授予角色功能
    message.info('批量授予角色功能开发中');
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
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      ellipsis: true,
      sorter: true,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      ellipsis: true,
      hideInSearch: true,
      render: (text) => text || '-',
      sorter: true,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      ellipsis: true,
      hideInSearch: true,
      render: (text) => text || '-',
      sorter: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      valueType: 'select',
      valueEnum: {
        1: {
          text: '启用',
          status: 'Success',
        },
        0: {
          text: '禁用',
          status: 'Error',
        },
      },
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
      sorter: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      sorter: true,
      width: 180,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      valueType: 'dateTime',
      hideInSearch: true,
      sorter: true,
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      valueType: 'option',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button 
            type="text" 
            icon={<EditOutlined />} 
            size="small"
            title="编辑"
            onClick={() => handleEditUser(record)}
          />
          <Button 
            type="text" 
            icon={<PoweroffOutlined />}
            size="small"
            title={record.status === 1 ? '禁用' : '启用'}
            danger={record.status === 1}
            onClick={() => handleChangeStatus(record)}
          />
          <Button 
            type="text" 
            icon={<SafetyOutlined />} 
            size="small"
            title="授予角色"
            onClick={() => handleAssignRoles(record)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div className="user-list-page">
      {/* 统计卡片 */}
      {statsVisible && (
        <div className="stats-grid">
        <Card className="stat-card">
          <Statistic
            title="用户总数"
            value={stats.total}
            prefix={<UserOutlined style={{ color: '#3f8cff' }} />}
            styles={{ content: { color: '#0a1629' } }}
          />
        </Card>
        <Card className="stat-card">
          <Statistic
            title="已启用"
            value={stats.enabled}
            prefix={<CheckCircleOutlined style={{ color: '#22c55e' }} />}
            styles={{ content: { color: '#0a1629' } }}
          />
        </Card>
        <Card className="stat-card">
          <Statistic
            title="已禁用"
            value={stats.disabled}
            prefix={<StopOutlined style={{ color: '#fb923c' }} />}
            styles={{ content: { color: '#0a1629' } }}
          />
        </Card>
        <Card className="stat-card">
          <Statistic
            title="今日新增"
            value={stats.today}
            prefix={<PlusOutlined style={{ color: '#a855f7' }} />}
            styles={{ content: { color: '#0a1629' } }}
          />
        </Card>
        </div>
      )}

      {/* 搜索表单区域 */}
      <div className="search-section">
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
      {selectedRowKeys.length > 0 && (
        <div className="batch-actions-section">
          <div className="batch-actions-info">
            <span className="selected-count">已选择 {selectedRowKeys.length} 项</span>
          </div>
          <div className="batch-actions-buttons">
            <Space>
              <Button
                icon={<CheckCircleOutlined />}
                onClick={() => handleBatchChangeStatus(1)}
              >
                批量启用
              </Button>
              <Button
                icon={<StopOutlined />}
                onClick={() => handleBatchChangeStatus(0)}
              >
                批量禁用
              </Button>
              <Button
                icon={<SafetyOutlined />}
                onClick={handleBatchAssignRoles}
              >
                批量授予角色
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                onClick={handleBatchDelete}
              >
                批量删除
              </Button>
              <Button
                type="text"
                onClick={() => setSelectedRowKeys([])}
              >
                取消选择
              </Button>
            </Space>
          </div>
        </div>
      )}

      {/* 数据表格 */}
      <ProTableV2
        actionRef={actionRef}
        headerTitle="用户列表"
        columns={columns}
        request={fetchUsers}
        rowKey="id"
        onDataChange={handleDataChange}
        search={false}
        defaultSort={{
          createTime: 'desc',
        }}
        rowSelection={rowSelection}
        toolbar={{
          actions: [
            <Button 
              key="add" 
              type="primary" 
              icon={<PlusOutlined />} 
              onClick={handleAddUser}
            >
              新增用户
            </Button>,
          ],
        }}
        params={filterParams}
      />

      {/* 新增/编辑用户抽屉 */}
      <Drawer
        title={editingUser ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onClose={handleCancel}
        width={560}
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
