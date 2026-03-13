import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthContext } from './hooks/AuthProvider.jsx';
import Login from './pages/Login.jsx';
import UserList from './pages/UserList.jsx';
import RoleList from './pages/RoleList.jsx';
import PermissionList from './pages/PermissionList.jsx';
import RolePermConfig from './pages/RolePermConfig.jsx';
import PostList from './pages/PostList.jsx';
import DictList from './pages/DictList.jsx';
import Profile from './pages/Profile.jsx';
import MenuList from './pages/MenuList.jsx';
import DeptList from './pages/DeptList.jsx';
import SessionList from './pages/SessionList.jsx';
import NotificationCenter from './pages/NotificationCenter.jsx';
import ComponentShowcase from './pages/ComponentShowcase.jsx';
import AppLayout from './components/AppLayout.jsx';

function PrivateRoute({ children }) {
  const { token } = useAuthContext();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={(
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        )}
      >
        {/* 默认首页重定向到用户管理 */}
        <Route index element={<Navigate to="/system/user" replace />} />
        
        {/* 系统管理 */}
        <Route path="system/user" element={<UserList />} />
        <Route path="system/role" element={<RoleList />} />
        <Route path="system/role/config" element={<RolePermConfig />} />
        <Route path="system/permission" element={<PermissionList />} />
        <Route path="system/menu" element={<MenuList />} />
        <Route path="system/dept" element={<DeptList />} />
        <Route path="system/post" element={<PostList />} />
        <Route path="system/dict" element={<DictList />} />
        <Route path="profile" element={<Profile />} />
        
        {/* 数据分析 */}
        <Route path="dashboard" element={<UserList />} />
        
        {/* 会话管理 */}
        <Route path="sessions" element={<SessionList />} />
        
        {/* 公共页面 */}
        <Route path="notifications" element={<NotificationCenter />} />
        <Route path="components" element={<ComponentShowcase />} />
        
        {/* 兼容旧路径 */}
        <Route path="roles" element={<Navigate to="/system/role" replace />} />
        <Route path="permissions" element={<Navigate to="/system/permission" replace />} />
      </Route>
    </Routes>
  );
}
