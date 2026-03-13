import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect, useRef, useMemo, useCallback, createContext, useContext } from 'react';
import { Button, Dropdown, Avatar, Badge, Breadcrumb, Empty, Spin, Drawer, List, Tabs, Input, Tag, Popconfirm, Space } from 'antd';
import { 
  // SearchOutlined, // 搜索框已移除
  BellOutlined, 
  UserOutlined, 
  LogoutOutlined,
  DownOutlined,
  HomeOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  MailOutlined,
  SettingOutlined,
  SafetyOutlined,
  UserSwitchOutlined,
  FileTextOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UpOutlined,
  ProfileOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  DeleteOutlined,
  ReadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useAuthContext } from '../hooks/AuthProvider.jsx';
import request from '../api/index.js';
import SidebarMenu from './SidebarMenu.jsx';
import { formatDateTime } from '../utils/dateUtils.js';
import './AppLayout.css';

const { Search } = Input;

const ICON_MAP = {
  // Ant Design icon names
  HomeOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  MailOutlined,
  SettingOutlined,
  SafetyOutlined,
  UserSwitchOutlined,
  FileTextOutlined,
  // Backend icon names → Ant Design mapping
  IconSettings: SettingOutlined,
  IconUser: UserOutlined,
  IconUserGroup: TeamOutlined,
  IconLock: SafetyOutlined,
  IconMenu: AppstoreOutlined,
  IconBranch: TeamOutlined,
  IconDashboard: HomeOutlined,
};

const getIconComponent = (iconName) => {
  if (!iconName) {
    return undefined;
  }
  return ICON_MAP[iconName] || FileTextOutlined;
};

// 退出登录菜单项
const logoutItem = {
  key: 'logout',
  label: '退出登录',
  path: '#',
  icon: LogoutOutlined,
};

// 创建页面工具栏 Context
const PageToolbarContext = createContext(null);

export const usePageToolbar = () => {
  const context = useContext(PageToolbarContext);
  return context;
};

export default function AppLayout() {
  const { user, setUser, setToken, token } = useAuthContext();
  const navigate = useNavigate();
  const location = useLocation();
  // const [searchValue, setSearchValue] = useState(''); // 搜索框已移除
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [menuTree, setMenuTree] = useState([]);
  const [menuLoading, setMenuLoading] = useState(false);
  const [pathMetaMap, setPathMetaMap] = useState({});
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [statsVisible, setStatsVisible] = useState(true); // 统计卡片显示/隐藏状态
  const [showStatsToggle, setShowStatsToggle] = useState(false); // 是否显示统计切换按钮
  const userMenuRef = useRef(null);
  const [notificationDrawerVisible, setNotificationDrawerVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [filteredNotifications, setFilteredNotifications] = useState([]);
  const [notificationLoading, setNotificationLoading] = useState(false);
  const [notificationActiveTab, setNotificationActiveTab] = useState('all');
  const [notificationSearchKeyword, setNotificationSearchKeyword] = useState('');
  const [unreadCount, setUnreadCount] = useState(0);

  const sortMenus = useCallback((menus = []) => {
    return [...menus].sort((a, b) => {
      const sortA = a?.sort ?? 0;
      const sortB = b?.sort ?? 0;
      if (sortA !== sortB) {
        return sortA - sortB;
      }
      const idA = a?.id ?? 0;
      const idB = b?.id ?? 0;
      return idA - idB;
    });
  }, []);

  const transformMenuData = useCallback((menus = []) => {
    if (!Array.isArray(menus)) {
      return [];
    }
    return sortMenus(menus).map(item => {
      const key = item?.id ? String(item.id) : (item?.code || item?.path || Math.random().toString(36).slice(2));
      const menuItem = {
        key,
        label: item?.menuName || item?.name || key,
        path: item?.path,
        icon: getIconComponent(item?.icon),
        sort: item?.sortOrder || item?.sort || 0,
      };
      if (Array.isArray(item?.children) && item.children.length > 0) {
        menuItem.children = transformMenuData(item.children);
      }
      return menuItem;
    });
  }, [sortMenus]);

  const extractPathMeta = useCallback((menus = []) => {
    const map = {};
    const traverse = (items = []) => {
      items.forEach(item => {
        if (item.path) {
          map[item.path] = {
            label: item.label,
            icon: item.icon,
          };
        }
        if (item.children && item.children.length > 0) {
          traverse(item.children);
        }
      });
    };
    traverse(menus);
    return map;
  }, []);

  // 用户信息在登录时已获取，不需要额外请求
  useEffect(() => {
    // placeholder
  }, [token, user]);

  const fetchMenus = useCallback(async () => {
    if (!token) {
      setMenuTree([]);
      setPathMetaMap({});
      return;
    }
    setMenuLoading(true);
    try {
      const res = await request.get('/system/menu/user-menus');
      if (res.code === 200 && Array.isArray(res.data)) {
        const transformed = transformMenuData(res.data);
        setMenuTree(transformed);
        setPathMetaMap(extractPathMeta(transformed));
      } else {
        setMenuTree([]);
        setPathMetaMap({});
      }
    } catch (error) {
      console.error('获取菜单失败:', error);
      setMenuTree([]);
      setPathMetaMap({});
    } finally {
      setMenuLoading(false);
    }
  }, [token, transformMenuData, extractPathMeta]);

  useEffect(() => {
    fetchMenus();
  }, [fetchMenus]);

  // 通知类型配置
  const notificationTypes = useMemo(() => ({
    info: { icon: <InfoCircleOutlined />, color: '#3f8cff', text: '信息' },
    success: { icon: <CheckCircleOutlined />, color: '#22c55e', text: '成功' },
    warning: { icon: <WarningOutlined />, color: '#fb923c', text: '警告' },
    error: { icon: <CloseCircleOutlined />, color: '#ef4444', text: '错误' },
  }), []);

  // 加载通知列表
  const loadNotifications = useCallback(async () => {
    setNotificationLoading(true);
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
    } finally {
      setNotificationLoading(false);
    }
  }, []);

  // 初始加载通知数据（用于显示未读数量）
  useEffect(() => {
    if (token) {
      loadNotifications();
    }
  }, [token, loadNotifications]);

  const allowedPaths = useMemo(() => {
    const paths = [];
    const traverse = (items = []) => {
      items.forEach(item => {
        if (item.path) {
          paths.push(item.path);
        }
        if (item.children && item.children.length > 0) {
          traverse(item.children);
        }
      });
    };
    traverse(menuTree);
    return paths;
  }, [menuTree]);

  // 允许访问的公共路径（不在菜单树中但应该允许访问的页面）
  const publicPaths = useMemo(() => [
    '/profile',           // 个人中心
    '/notifications',      // 通知中心
    '/components',         // 组件展示
  ], []);

  useEffect(() => {
    if (!token || menuLoading || allowedPaths.length === 0) {
      return;
    }
    const currentPath = location.pathname;
    // 检查是否是公共路径
    const isPublicPath = publicPaths.some(path => currentPath === path || currentPath.startsWith(`${path}/`));
    // 检查是否在允许的路径中
    const matched = allowedPaths.some(path => currentPath === path || currentPath.startsWith(`${path}/`));
    // 如果是公共路径或在允许的路径中，则不重定向
    if (!isPublicPath && !matched) {
      navigate(allowedPaths[0], { replace: true });
    }
  }, [allowedPaths, location.pathname, menuLoading, navigate, token, publicPaths]);

  const currentBreadcrumb = useMemo(() => {
    const currentPath = location.pathname;
    if (!currentPath) {
      return null;
    }
    let matched = null;
    Object.entries(pathMetaMap).forEach(([path, meta]) => {
      if (currentPath === path || currentPath.startsWith(`${path}/`)) {
        if (!matched || path.length > matched.path.length) {
          matched = { path, ...meta };
        }
      }
    });
    
    // 添加页面描述信息
    const pageDescriptions = {
      '/': '管理系统用户信息、角色和权限设置',
      '/roles': '管理系统角色信息、权限配置',
      '/permissions': '管理系统权限配置、菜单管理',
      '/sessions': '管理系统用户会话、在线状态',
      '/profile': '管理您的个人信息和账户设置',
      '/notifications': '查看和管理系统通知消息',
      '/components': '查看项目中所有可用的组件及其使用示例',
    };
    
    if (matched) {
      matched.description = pageDescriptions[matched.path] || '';
    }
    
    return matched;
  }, [location.pathname, pathMetaMap]);

  // 生成面包屑数据
  const breadcrumbItems = useMemo(() => {
    const items = [
      {
        title: (
          <span 
            style={{ display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer' }}
            onClick={() => navigate('/')}
          >
            <HomeOutlined />
            <span>首页</span>
          </span>
        ),
      },
    ];

    if (currentBreadcrumb && currentBreadcrumb.path !== '/') {
      const IconComponent = currentBreadcrumb.icon || FileTextOutlined;
      items.push({
        title: (
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            {IconComponent ? <IconComponent /> : <FileTextOutlined />}
            <span>{currentBreadcrumb.label}</span>
          </span>
        ),
      });
    }

    return items;
  }, [currentBreadcrumb, navigate]);

  // 打开通知抽屉时加载通知
  useEffect(() => {
    if (notificationDrawerVisible) {
      loadNotifications();
    }
  }, [notificationDrawerVisible, loadNotifications]);

  // 筛选通知
  useEffect(() => {
    let filtered = [...notifications];

    // 按标签筛选
    if (notificationActiveTab === 'unread') {
      filtered = filtered.filter(n => !n.read);
    } else if (notificationActiveTab !== 'all') {
      filtered = filtered.filter(n => n.type === notificationActiveTab);
    }

    // 按关键词搜索
    if (notificationSearchKeyword) {
      const keyword = notificationSearchKeyword.toLowerCase();
      filtered = filtered.filter(n => 
        n.title.toLowerCase().includes(keyword) ||
        n.content.toLowerCase().includes(keyword)
      );
    }

    setFilteredNotifications(filtered);
  }, [notifications, notificationActiveTab, notificationSearchKeyword]);

  // 标记为已读
  const markNotificationAsRead = useCallback(async (id) => {
    try {
      // const res = await request.put(`/notifications/${id}/read`);
      setNotifications(prev => 
        prev.map(n => n.id === id ? { ...n, read: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('markNotificationAsRead error:', error);
    }
  }, []);

  // 标记全部为已读
  const markAllNotificationsAsRead = useCallback(async () => {
    try {
      // const res = await request.put('/notifications/read-all');
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('markAllNotificationsAsRead error:', error);
    }
  }, []);

  // 删除通知
  const deleteNotification = useCallback(async (id) => {
    try {
      // const res = await request.delete(`/notifications/${id}`);
      setNotifications(prev => {
        const deleted = prev.find(n => n.id === id);
        if (deleted && !deleted.read) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
        return prev.filter(n => n.id !== id);
      });
    } catch (error) {
      console.error('deleteNotification error:', error);
    }
  }, []);

  // 清空已读通知
  const clearReadNotifications = useCallback(async () => {
    try {
      // const res = await request.delete('/notifications/read');
      setNotifications(prev => prev.filter(n => !n.read));
    } catch (error) {
      console.error('clearReadNotifications error:', error);
    }
  }, []);

  // 点击外部区域关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    };

    if (showUserMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showUserMenu]);

  const handleLogout = async () => {
    try {
      // 根据 OpenAPI 规范：/api/auth/logout POST
      // 调用登出接口
      await request.post('/auth/logout');
    } catch (error) {
      console.error('登出接口调用失败:', error);
      // 即使接口调用失败，也清除本地状态
    } finally {
      // 清除本地存储和状态
      setToken(null);
      setUser(null);
      localStorage.removeItem('uc_token');
      localStorage.removeItem('uc_user');
      localStorage.removeItem('uc_remember');
      navigate('/login', { replace: true });
    }
  };

  const handleMenuClick = (item) => {
    if (item.key === 'logout') {
      handleLogout();
    }
  };

  // const handleSearch = (e) => {
  //   e.preventDefault();
  //   // 搜索逻辑可以在这里实现
  //   console.log('搜索:', searchValue);
  // }; // 搜索框已移除

  // 获取用户头像显示内容
  const getUserAvatarProps = () => {
    // 检查 avatar 是否存在且不为空字符串
    if (user?.avatar && typeof user.avatar === 'string' && user.avatar.trim() !== '') {
      return { src: user.avatar };
    }
    // 如果有昵称或用户名，显示首字母
    const name = user?.nickname || user?.username || '';
    if (name) {
      return { 
        style: { backgroundColor: '#3f8cff', color: 'white' },
        children: name.charAt(0).toUpperCase()
      };
    }
    // 默认显示图标
    return { 
      icon: <UserOutlined />,
      style: { backgroundColor: '#3f8cff', color: 'white' }
    };
  };

  // 用户下拉菜单项
  const userMenuItems = [
    {
      key: 'user-info',
      label: (
        <div className="user-menu-header">
          <Avatar 
            size="large"
            {...getUserAvatarProps()}
          />
          <div className="user-menu-info">
            <div className="user-menu-name">{user?.nickname || user?.username || 'User'}</div>
            <div className="user-menu-email">{user?.email || user?.phone || ''}</div>
          </div>
        </div>
      ),
      disabled: true,
      className: 'user-menu-item-info',
    },
    {
      type: 'divider',
      className: 'user-menu-divider',
    },
    {
      key: 'profile',
      label: (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ProfileOutlined />
          <span>个人中心</span>
        </div>
      ),
      className: 'user-menu-item',
    },
    {
      type: 'divider',
      className: 'user-menu-divider',
    },
    {
      key: 'logout',
      label: (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <LogoutOutlined />
          <span>退出登录</span>
        </div>
      ),
      className: 'user-menu-item',
    },
  ];

  return (
    <div className="app-layout">
      {/* 左侧菜单栏 */}
      <aside className={`sidebar ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
        <div className="sidebar-content">
          {/* Logo 区域 */}
          <div className="sidebar-logo">
            <div className="logo-icon">
              <div style={{ 
                width: '32px', 
                height: '32px', 
                borderRadius: '8px', 
                background: '#3f8cff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '18px',
                fontWeight: 'bold'
              }}>
                U
              </div>
            </div>
            {!sidebarCollapsed && (
              <div className="logo-text">
                <div className="logo-title">用户中心</div>
                <div className="logo-subtitle">User Center</div>
              </div>
            )}
            {/* 折叠按钮 */}
            <Button
              type="text"
              icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="collapse-toggle-btn"
              title={sidebarCollapsed ? "展开菜单" : "折叠菜单"}
            />
          </div>
          
          <div style={{ flex: 1, width: '100%' }}>
            {menuLoading ? (
              <div style={{ display: 'flex', justifyContent: 'center', padding: '32px 0' }}>
                <Spin />
              </div>
            ) : menuTree.length > 0 ? (
              <SidebarMenu items={menuTree} onItemClick={handleMenuClick} collapsed={sidebarCollapsed} />
            ) : (
              <Empty
                description={sidebarCollapsed ? "" : "暂无可用菜单"}
                image={sidebarCollapsed ? null : Empty.PRESENTED_IMAGE_SIMPLE}
                style={{ padding: '24px 0' }}
              />
            )}
          </div>
          <div className="sidebar-footer">
            <SidebarMenu items={[logoutItem]} onItemClick={handleMenuClick} collapsed={sidebarCollapsed} />
          </div>
        </div>
      </aside>

      {/* 主内容区 */}
      <main className={`main-content ${sidebarCollapsed ? 'main-content-collapsed' : ''}`}>
        <header className="toolbar">
          <div className="toolbar-content">
            {/* 左侧页面标题 */}
            {currentBreadcrumb && (
              <div className="toolbar-title-section">
                <div className="toolbar-title-content">
                  <h2 className="toolbar-title">{currentBreadcrumb.label}</h2>
                  {currentBreadcrumb.description && (
                    <p className="toolbar-description">{currentBreadcrumb.description}</p>
                  )}
                </div>
              </div>
            )}

            {/* 右侧工具栏 */}
            <div className="toolbar-actions">
              {/* 统计显示/隐藏按钮 */}
              {showStatsToggle && (
                <Button
                  type="text"
                  icon={statsVisible ? <UpOutlined /> : <DownOutlined />}
                  onClick={() => setStatsVisible(!statsVisible)}
                  className="toggle-stats-btn"
                  title={statsVisible ? '隐藏统计' : '显示统计'}
                >
                  {statsVisible ? '隐藏统计' : '显示统计'}
                </Button>
              )}
              
              {/* 通知按钮 */}
              <Badge count={unreadCount} size="small" offset={[-2, 2]}>
                <Button
                  type="text"
                  icon={<BellOutlined />}
                  className="toolbar-btn-icon"
                  title="通知"
                  onClick={() => setNotificationDrawerVisible(true)}
                />
              </Badge>

              {/* 用户资料下拉 */}
              <Dropdown
                menu={{ 
                  items: userMenuItems,
                  onClick: ({ key }) => {
                    if (key === 'profile') {
                      navigate('/profile');
                    } else if (key === 'logout') {
                      handleLogout();
                    }
                  }
                }}
                trigger={['click']}
                placement="bottomRight"
                classNames={{ root: 'user-profile-dropdown-menu' }}
              >
                <Button
                  type="text"
                  className="toolbar-btn-user"
                >
                  <Avatar 
                    size={32}
                    {...getUserAvatarProps()}
                  />
                  <span className="user-name">{user?.nickname || user?.username || 'User'}</span>
                  <DownOutlined className="user-dropdown-icon" />
                </Button>
              </Dropdown>
            </div>
          </div>
        </header>

        {/* 面包屑导航 */}
        <nav className="breadcrumb-nav">
          <Breadcrumb
            items={breadcrumbItems}
            separator="/"
            className="app-breadcrumb"
          />
        </nav>

        <section className="content">
          <PageToolbarContext.Provider value={{ statsVisible, setStatsVisible, setShowStatsToggle }}>
            <Outlet />
          </PageToolbarContext.Provider>
        </section>
      </main>

      {/* 通知中心抽屉 */}
      <Drawer
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <BellOutlined />
              <span>通知中心</span>
              {unreadCount > 0 && (
                <Badge count={unreadCount} size="small" style={{ marginLeft: '8px' }} />
              )}
            </div>
            <Space>
              {unreadCount > 0 && (
                <Button 
                  type="text" 
                  size="small"
                  icon={<ReadOutlined />} 
                  onClick={markAllNotificationsAsRead}
                >
                  全部已读
                </Button>
              )}
              <Popconfirm
                title="确定要清空所有已读通知吗？"
                onConfirm={clearReadNotifications}
                okText="确定"
                cancelText="取消"
              >
                <Button type="text" size="small" danger icon={<DeleteOutlined />}>
                  清空已读
                </Button>
              </Popconfirm>
            </Space>
          </div>
        }
        placement="right"
        size={480}
        open={notificationDrawerVisible}
        onClose={() => setNotificationDrawerVisible(false)}
        className="notification-drawer"
      >
        <div className="notification-drawer-content">
          <Search
            placeholder="搜索通知标题或内容"
            allowClear
            prefix={<SearchOutlined />}
            value={notificationSearchKeyword}
            onChange={(e) => setNotificationSearchKeyword(e.target.value)}
            className="notification-search-input"
          />
          
          <Tabs
            activeKey={notificationActiveTab}
            onChange={setNotificationActiveTab}
            items={[
              { key: 'all', label: `全部 (${notifications.length})` },
              { key: 'unread', label: `未读 (${unreadCount})` },
              { key: 'info', label: '信息' },
              { key: 'success', label: '成功' },
              { key: 'warning', label: '警告' },
              { key: 'error', label: '错误' },
            ]}
          />

          <List
            loading={notificationLoading}
            dataSource={filteredNotifications}
            locale={{ emptyText: <Empty description="暂无通知" /> }}
            style={{ flex: 1, overflow: 'auto' }}
            renderItem={(item) => {
              const typeConfig = notificationTypes[item.type] || notificationTypes.info;
              return (
                <List.Item
                  className={item.read ? 'notification-item-read' : 'notification-item-unread'}
                  style={{
                    padding: '12px',
                    borderBottom: '1px solid var(--border-light)',
                    backgroundColor: item.read ? 'transparent' : 'var(--info-bg)',
                    borderLeft: item.read ? 'none' : `3px solid ${typeConfig.color}`,
                  }}
                  actions={[
                    !item.read && (
                      <Button
                        type="text"
                        size="small"
                        icon={<ReadOutlined />}
                        onClick={() => markNotificationAsRead(item.id)}
                      >
                        已读
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
                        style={{
                          fontSize: '1.5rem',
                          color: typeConfig.color,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: '40px',
                          height: '40px',
                          borderRadius: 'var(--radius-sm)',
                          backgroundColor: 'var(--bg-tertiary)',
                        }}
                      >
                        {typeConfig.icon}
                      </div>
                    }
                    title={
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {!item.read && <Badge dot style={{ marginRight: '4px' }} />}
                        <span style={{ fontWeight: 600 }}>{item.title}</span>
                        <Tag color={typeConfig.color} style={{ fontSize: '12px' }}>
                          {typeConfig.text}
                        </Tag>
                      </div>
                    }
                    description={
                      <div>
                        <div style={{ marginBottom: '4px', lineHeight: 1.6 }}>{item.content}</div>
                        <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>
                          {formatDateTime(item.createTime)}
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              );
            }}
          />
        </div>
      </Drawer>
    </div>
  );
}
