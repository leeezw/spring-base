import { useState, useRef, useCallback, useEffect } from 'react';
import { Card, Statistic, Button, Space, Tag, Modal, Form, message, Tooltip, Select, Input } from 'antd';
import { 
  DesktopOutlined, 
  MobileOutlined,
  TabletOutlined,
  GlobalOutlined,
  LogoutOutlined,
  PoweroffOutlined,
  StopOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import TableSearchForm from '../components/TableSearchForm.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';
import { usePageToolbar } from '../components/AppLayout.jsx';
import { formatDateTime } from '../utils/dateUtils.js';
import './SessionList.css';

export default function SessionList() {
  const actionRef = useRef();
  const [stats, setStats] = useState({
    total: 0,
    active: 0,
    inactive: 0,
  });
  const [filterForm] = Form.useForm();
  const [filterParams, setFilterParams] = useState({});
  const [users, setUsers] = useState([]);
  const [revokeModalVisible, setRevokeModalVisible] = useState(false);
  const [revokeLoading, setRevokeLoading] = useState(false);
  const [revokeForm] = Form.useForm();
  const { statsVisible, setStatsVisible, setShowStatsToggle } = usePageToolbar();
  const debounceTimerRef = useRef(null);
  
  // 组件挂载时显示统计切换按钮
  useEffect(() => {
    setShowStatsToggle(true);
    return () => {
      setShowStatsToggle(false);
    };
  }, [setShowStatsToggle]);
  
  const sessionStatusMap = {
    1: { text: '活跃', color: 'success' },
    0: { text: '已禁用', color: 'default' },
    [-1]: { text: '强制下线', color: 'error' },
    [-2]: { text: '设备已踢出', color: 'warning' },
    [-3]: { text: '已被顶替', color: 'warning' },
  };

  // 防抖处理筛选
  const handleFilterChange = useCallback(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
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

  // 加载用户列表（用于筛选）
  useEffect(() => {
    const loadUsers = async () => {
      try {
        const res = await request.get('/system/user/page', { params: { pageSize: 1000 } });
        if (res.code === 200 && res.data && res.data.pageData) {
          const userList = Array.isArray(res.data.pageData.list) ? res.data.pageData.list : [];
          setUsers(userList);
        }
      } catch (error) {
        console.error('loadUsers error:', error);
      }
    };
    loadUsers();
  }, []);

  // 获取设备图标
  const getDeviceIcon = (deviceId = '') => {
    const deviceLower = deviceId.toLowerCase();
    if (deviceLower.includes('mobile') || deviceLower.includes('phone') || deviceLower.includes('android') || deviceLower.includes('ios')) {
      return <MobileOutlined />;
    }
    if (deviceLower.includes('tablet') || deviceLower.includes('ipad')) {
      return <TabletOutlined />;
    }
    if (deviceLower.includes('desktop') || deviceLower.includes('windows') || deviceLower.includes('mac')) {
      return <DesktopOutlined />;
    }
    return <GlobalOutlined />;
  };
  
  const normalizeTimestamp = (value) => {
    if (!value) return null;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    return date.toISOString();
  };

  // 请求函数 - 获取所有用户的Session
  const fetchSessions = async (params) => {
    try {
      const payload = {
        pageNum: params.current || params.pageNum || 1,
        pageSize: params.pageSize || 10,
      };
      if (params.keyword) {
        payload.keyword = params.keyword;
      }
      if (params.userId) {
        payload.userId = params.userId;
      }
      if (params.startTime) {
        payload.startTime = params.startTime;
      }
      if (params.endTime) {
        payload.endTime = params.endTime;
      }
      const res = await request.post('/auth/session/list', payload);
      if (res.code === 200 && res.data) {
        const list = Array.isArray(res.data.list) ? res.data.list : [];
        const formattedList = list.map(item => ({
          ...item,
          id: item.id || `${item.userId || 'user'}-${item.sessionKey}`,
          startTime: normalizeTimestamp(item.startTime),
          lastAccessTime: normalizeTimestamp(item.lastAccessTime),
          operationTime: normalizeTimestamp(item.operationTime),
        }));
        const total = typeof res.data.total === 'number' ? res.data.total : formattedList.length;
        const activeCount = formattedList.filter(item => (item.status ?? 1) === 1).length;
        setStats({
          total,
          active: activeCount,
          inactive: Math.max(total - activeCount, 0),
        });
        return {
          code: 200,
          data: {
            list: formattedList,
            total,
          }
        };
      }
      return {
        code: 500,
        data: { list: [], total: 0 }
      };
    } catch (error) {
      console.error('fetchSessions error:', error);
      return {
        code: 500,
        data: { list: [], total: 0 }
      };
    }
  };

  const handleRefresh = () => {
    actionRef.current?.reload();
  };

  // 强制用户下线
  const handleKickOutUser = (record) => {
    Modal.confirm({
      title: '确认强制下线',
      content: `确定要强制用户 "${record.nickname || record.username}" 下线吗？这将踢出该用户的所有设备。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await request.post(`/auth/session/kick-out/${record.userId}`);
          if (res.code === 200) {
            message.success('用户已强制下线');
            handleRefresh();
          } else {
            message.error(res.message || '操作失败');
          }
        } catch (error) {
          message.error(error.message || '操作失败');
        }
      }
    });
  };

  // 踢出指定设备
  const handleKickOutDevice = (record) => {
    Modal.confirm({
      title: '确认踢出设备',
      content: `确定要踢出设备 "${record.deviceId}" 吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await request.post(`/auth/session/kick-out-device?userId=${record.userId}&deviceId=${record.deviceId}`);
          if (res.code === 200) {
            message.success('设备已踢出');
            handleRefresh();
          } else {
            message.error(res.message || '操作失败');
          }
        } catch (error) {
          message.error(error.message || '操作失败');
        }
      }
    });
  };

  // 禁用用户
  const handleDisableUser = (record) => {
    Modal.confirm({
      title: '确认禁用用户',
      content: `确定要禁用用户 "${record.nickname || record.username}" 吗？该用户所有 Session 将立即失效。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await request.post(`/auth/session/disable/${record.userId}`);
          if (res.code === 200) {
            message.success('用户已禁用');
            handleRefresh();
          } else {
            message.error(res.message || '操作失败');
          }
        } catch (error) {
          message.error(error.message || '操作失败');
        }
      }
    });
  };

  const openRevokeModal = () => {
    revokeForm.resetFields();
    setRevokeModalVisible(true);
  };

  const handleRevokeToken = async () => {
    try {
      const values = await revokeForm.validateFields();
      setRevokeLoading(true);
      const query = new URLSearchParams({
        token: values.token,
        reason: values.reason || ''
      }).toString();
      const res = await request.post(`/auth/session/revoke-token?${query}`);
      if (res.code === 200) {
        message.success('Token 已撤销');
        setRevokeModalVisible(false);
      } else {
        message.error(res.message || '操作失败');
      }
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '操作失败');
    } finally {
      setRevokeLoading(false);
    }
  };

  const columns = [
    {
      title: '用户',
      key: 'user',
      width: 180,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 600 }}>{record.nickname || record.username}</div>
          <div style={{ fontSize: '12px', color: '#999' }}>{record.username}</div>
        </div>
      ),
    },
    {
      title: '会话信息',
      key: 'sessionInfo',
      width: 320,
      render: (_, record) => (
        <div className="session-info-cell">
          <Space align="start">
            {getDeviceIcon(record.deviceId)}
            <div className="session-info">
              <div className="session-device-text">{record.deviceId || '-'}</div>
              <Tooltip title={record.sessionKey}>
                <code className="session-key-text">{record.sessionKey}</code>
              </Tooltip>
            </div>
          </Space>
        </div>
      ),
    },
    {
      title: '时间线',
      key: 'timeline',
      width: 260,
      render: (_, record) => (
        <div className="session-timeline">
          <div>
            <span className="timeline-label">创建：</span>
            {formatDateTime(record.startTime)}
          </div>
          <div>
            <span className="timeline-label">最近活动：</span>
            {formatDateTime(record.lastAccessTime)}
          </div>
          <div>
            <span className="timeline-label">操作时间：</span>
            {formatDateTime(record.operationTime)}
          </div>
        </div>
      ),
    },
    {
      title: '状态',
      key: 'status',
      width: 140,
      render: (_, record) => {
        const meta = sessionStatusMap[record.status] || {};
        return (
          <Tag color={meta.color || 'default'}>
            {record.statusDesc || meta.text || '未知'}
          </Tag>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button 
            type="text" 
            icon={<LogoutOutlined />} 
            size="small"
            danger
            title="踢出设备"
            onClick={() => handleKickOutDevice(record)}
          />
          <Button 
            type="text" 
            icon={<PoweroffOutlined />} 
            size="small"
            danger
            title="强制用户下线"
            onClick={() => handleKickOutUser(record)}
          />
          <Button 
            type="text" 
            icon={<StopOutlined />} 
            size="small"
            danger
            title="禁用用户"
            onClick={() => handleDisableUser(record)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div className="session-list-page">
      {/* 统计卡片 */}
      {statsVisible && (
        <div className="stats-grid">
          <Card className="stat-card">
            <Statistic
              title="Session总数"
              value={stats.total}
              prefix={<DesktopOutlined style={{ color: '#3f8cff' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
          <Card className="stat-card">
            <Statistic
              title="活跃Session"
              value={stats.active}
              prefix={<GlobalOutlined style={{ color: '#22c55e' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
          <Card className="stat-card">
            <Statistic
              title="异常/下线"
              value={stats.inactive}
              prefix={<PoweroffOutlined style={{ color: '#f97316' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
        </div>
      )}

      {/* 搜索表单区域 */}
      <div className="search-section">
        <div className="session-search-form-wrapper">
          <TableSearchForm
            form={filterForm}
            onFinish={async (values) => {
              const newFilterParams = { ...values };
              // 处理日期时间范围参数
              if (values.dateRange) {
                newFilterParams.startTime = values.startTime;
                newFilterParams.endTime = values.endTime;
                // 移除 dateRange 对象，避免发送到后端
                delete newFilterParams.dateRange;
              }
              setFilterParams(newFilterParams);
              actionRef.current?.reload();
            }}
            onKeywordChange={handleFilterChange}
            config={{
              showKeyword: true,
              keywordPlaceholder: '搜索用户名、昵称、设备或Session Key',
              showStatus: false,
              showDateRange: true,
              dateRangePlaceholder: ['创建开始时间', '创建结束时间'],
              dateRangeStartName: 'startTime',
              dateRangeEndName: 'endTime',
            }}
          />
          <div className="user-select-wrapper">
            <Form form={filterForm}>
              <Form.Item name="userId" style={{ marginBottom: 0 }}>
                <Select
                  className='filter-status-select'
                  placeholder="选择用户"
                  allowClear
                  showSearch
                  optionFilterProp="children"
                  onChange={() => filterForm.submit()}
                  style={{ width: 200 }}
                >
                  {users.map(user => (
                    <Select.Option key={user.id} value={user.id}>
                      {user.nickname || user.username}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Form>
          </div>
        </div>
      </div>

      {/* 数据表格 */}
      <ProTableV2
        actionRef={actionRef}
        headerTitle="Session列表"
        columns={columns}
        request={fetchSessions}
        rowKey="id"
        search={false}
        toolbar={{
          actions: (
            <Button 
              type="primary" 
              icon={<SafetyCertificateOutlined />} 
              onClick={openRevokeModal}
            >
              撤销Token
            </Button>
          ),
        }}
        params={filterParams}
      />
      
      <Modal
        title="撤销 Token"
        open={revokeModalVisible}
        onCancel={() => setRevokeModalVisible(false)}
        confirmLoading={revokeLoading}
        onOk={handleRevokeToken}
        okText="确认撤销"
        cancelText="取消"
      >
        <Form form={revokeForm} layout="vertical">
          <Form.Item
            label="Token"
            name="token"
            rules={[{ required: true, message: '请输入要撤销的Token' }]}
          >
            <Input.TextArea rows={4} placeholder="粘贴需要撤销的Token" />
          </Form.Item>
          <Form.Item
            label="原因"
            name="reason"
          >
            <Input placeholder="可选，记录撤销原因" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
