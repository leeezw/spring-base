import {
  DashboardOutlined,
  SafetyCertificateOutlined,
  KeyOutlined,
  CloudOutlined,
  CloudUploadOutlined,
  ProfileOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { ProLayout } from '@ant-design/pro-components';
import { Dropdown } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { TOKEN_KEY } from '../api/request';

const menus = [
  { path: '/dashboard', name: '概览', icon: <DashboardOutlined /> },
  { path: '/certificates', name: '证书管理', icon: <SafetyCertificateOutlined /> },
  { path: '/accounts', name: 'ACME 账户', icon: <KeyOutlined /> },
  { path: '/dns', name: 'DNS 服务商', icon: <CloudOutlined /> },
  { path: '/targets', name: '部署目标', icon: <CloudUploadOutlined /> },
  { path: '/logs', name: '任务日志', icon: <ProfileOutlined /> },
];

export default function BasicLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <ProLayout
      title="证书自动化"
      logo={<SafetyCertificateOutlined style={{ fontSize: 24, color: '#1677ff' }} />}
      layout="mix"
      location={{ pathname: location.pathname }}
      route={{ path: '/', routes: menus }}
      menuItemRender={(item, dom) => <a onClick={() => navigate(item.path || '/dashboard')}>{dom}</a>}
      avatarProps={{
        title: '管理员',
        render: (_, dom) => (
          <Dropdown
            menu={{
              items: [{ key: 'logout', icon: <LogoutOutlined />, label: '退出登录' }],
              onClick: () => {
                localStorage.removeItem(TOKEN_KEY);
                navigate('/login');
              },
            }}
          >
            {dom}
          </Dropdown>
        ),
      }}
    >
      <Outlet />
    </ProLayout>
  );
}
