import { useState, useEffect } from 'react';
import { Card, List, Badge, Button, Space, Tag, Empty, Tabs, Input, message, Popconfirm } from 'antd';
import { 
  BellOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  DeleteOutlined,
  ReadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { formatDateTime } from '../utils/dateUtils.js';
import './NotificationCenter.css';

const { Search } = Input;

// 通知类型配置
const notificationTypes = {
  info: { icon: <InfoCircleOutlined />, color: '#3f8cff', text: '信息' },
  success: { icon: <CheckCircleOutlined />, color: '#22c55e', text: '成功' },
  warning: { icon: <WarningOutlined />, color: '#fb923c', text: '警告' },
  error: { icon: <CloseCircleOutlined />, color: '#ef4444', text: '错误' },
};

export default function NotificationCenter() {
  const [notifications, setNotifications] = useState([]);
  const [filteredNotifications, setFilteredNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('all');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [unreadCount, setUnreadCount] = useState(0);

  // 加载通知列表
  const loadNotifications = async () => {
    setLoading(true);
    try {
      // 这里应该调用实际的通知API
      // const res = await request.get('/notifications');
      // 模拟数据
      const mockData = [
        {
          id: 1,
          title: '系统更新通知',
          content: '系统将于今晚22:00进行维护更新，预计持续2小时。',
          type: 'info',
          read: false,
          createTime: new Date().toISOString(),
        },
        {
          id: 2,
          title: '密码修改成功',
          content: '您的密码已成功修改，如非本人操作，请立即联系管理员。',
          type: 'success',
          read: false,
          createTime: new Date(Date.now() - 3600000).toISOString(),
        },
        {
          id: 3,
          title: '登录异常警告',
          content: '检测到您的账号在异常地点登录，请确认是否为本人操作。',
          type: 'warning',
          read: true,
          createTime: new Date(Date.now() - 7200000).toISOString(),
        },
        {
          id: 4,
          title: '权限变更通知',
          content: '您的角色权限已更新，请重新登录以生效。',
          type: 'info',
          read: true,
          createTime: new Date(Date.now() - 86400000).toISOString(),
        },
      ];
      
      setNotifications(mockData);
      setFilteredNotifications(mockData);
      setUnreadCount(mockData.filter(n => !n.read).length);
    } catch (error) {
      console.error('loadNotifications error:', error);
      message.error('加载通知失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  // 筛选通知
  useEffect(() => {
    let filtered = [...notifications];

    // 按标签筛选
    if (activeTab === 'unread') {
      filtered = filtered.filter(n => !n.read);
    } else if (activeTab !== 'all') {
      filtered = filtered.filter(n => n.type === activeTab);
    }

    // 按关键词搜索
    if (searchKeyword) {
      const keyword = searchKeyword.toLowerCase();
      filtered = filtered.filter(n => 
        n.title.toLowerCase().includes(keyword) ||
        n.content.toLowerCase().includes(keyword)
      );
    }

    setFilteredNotifications(filtered);
  }, [notifications, activeTab, searchKeyword]);

  // 标记为已读
  const markAsRead = async (id) => {
    try {
      // const res = await request.put(`/notifications/${id}/read`);
      setNotifications(prev => 
        prev.map(n => n.id === id ? { ...n, read: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
      message.success('已标记为已读');
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 标记全部为已读
  const markAllAsRead = async () => {
    try {
      // const res = await request.put('/notifications/read-all');
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
      message.success('已全部标记为已读');
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 删除通知
  const deleteNotification = async (id) => {
    try {
      // const res = await request.delete(`/notifications/${id}`);
      setNotifications(prev => {
        const deleted = prev.find(n => n.id === id);
        if (deleted && !deleted.read) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
        return prev.filter(n => n.id !== id);
      });
      message.success('删除成功');
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 清空已读通知
  const clearRead = async () => {
    try {
      // const res = await request.delete('/notifications/read');
      setNotifications(prev => prev.filter(n => !n.read));
      message.success('已清空已读通知');
    } catch (error) {
      message.error('操作失败');
    }
  };

  return (
    <div className="notification-center-page">
      <div className="notification-header">
        <div className="header-title">
          <BellOutlined className="header-icon" />
          <h2>通知中心</h2>
          {unreadCount > 0 && (
            <Badge count={unreadCount} size="small" className="unread-badge">
              <span className="unread-text">{unreadCount} 条未读</span>
            </Badge>
          )}
        </div>
        <Space>
          {unreadCount > 0 && (
            <Button 
              icon={<ReadOutlined />} 
              onClick={markAllAsRead}
            >
              全部已读
            </Button>
          )}
          <Popconfirm
            title="确定要清空所有已读通知吗？"
            onConfirm={clearRead}
            okText="确定"
            cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />}>
              清空已读
            </Button>
          </Popconfirm>
        </Space>
      </div>

      <Card className="notification-content-card">
        <div className="notification-toolbar">
          <Search
            placeholder="搜索通知标题或内容"
            allowClear
            prefix={<SearchOutlined />}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="notification-search"
          />
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            className="notification-tabs"
            items={[
              { key: 'all', label: `全部 (${notifications.length})` },
              { key: 'unread', label: `未读 (${unreadCount})` },
              { key: 'info', label: '信息' },
              { key: 'success', label: '成功' },
              { key: 'warning', label: '警告' },
              { key: 'error', label: '错误' },
            ]}
          />
        </div>

        <List
          loading={loading}
          dataSource={filteredNotifications}
          locale={{ emptyText: <Empty description="暂无通知" /> }}
          renderItem={(item) => {
            const typeConfig = notificationTypes[item.type] || notificationTypes.info;
            return (
              <List.Item
                className={`notification-item ${item.read ? 'read' : 'unread'}`}
                actions={[
                  !item.read && (
                    <Button
                      type="text"
                      size="small"
                      icon={<ReadOutlined />}
                      onClick={() => markAsRead(item.id)}
                    >
                      标记已读
                    </Button>
                  ),
                  <Popconfirm
                    title="确定要删除这条通知吗？"
                    onConfirm={() => deleteNotification(item.id)}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                    >
                      删除
                    </Button>
                  </Popconfirm>,
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  avatar={
                    <div
                      className="notification-icon"
                      style={{ color: typeConfig.color }}
                    >
                      {typeConfig.icon}
                    </div>
                  }
                  title={
                    <div className="notification-title">
                      {!item.read && <Badge dot className="unread-dot" />}
                      <span>{item.title}</span>
                      <Tag color={typeConfig.color} className="type-tag">
                        {typeConfig.text}
                      </Tag>
                    </div>
                  }
                  description={
                    <div>
                      <div className="notification-content">{item.content}</div>
                      <div className="notification-time">
                        {formatDateTime(item.createTime)}
                      </div>
                    </div>
                  }
                />
              </List.Item>
            );
          }}
        />
      </Card>
    </div>
  );
}

