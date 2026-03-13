import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuthContext } from './hooks/AuthProvider.jsx';
import Login from './pages/Login.jsx';
import AppLayout from './components/AppLayout.jsx';

// 路由级懒加载
const Dashboard = lazy(() => import('./pages/Dashboard.jsx'));
const UserList = lazy(() => import('./pages/UserList.jsx'));
const RoleList = lazy(() => import('./pages/RoleList.jsx'));
const PermissionList = lazy(() => import('./pages/PermissionList.jsx'));
const RolePermConfig = lazy(() => import('./pages/RolePermConfig.jsx'));
const PostList = lazy(() => import('./pages/PostList.jsx'));
const DictList = lazy(() => import('./pages/DictList.jsx'));
const MenuList = lazy(() => import('./pages/MenuList.jsx'));
const DeptList = lazy(() => import('./pages/DeptList.jsx'));
const Profile = lazy(() => import('./pages/Profile.jsx'));

// 懒加载 fallback
const PageLoading = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
    <Spin size="large" />
  </div>
);

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
        {/* 默认首页 */}
        <Route index element={<Navigate to="/dashboard" replace />} />
        
        {/* 数据看板 */}
        <Route path="dashboard" element={<Suspense fallback={<PageLoading />}><Dashboard /></Suspense>} />
        
        {/* 系统管理 */}
        <Route path="system/user" element={<Suspense fallback={<PageLoading />}><UserList /></Suspense>} />
        <Route path="system/role" element={<Suspense fallback={<PageLoading />}><RoleList /></Suspense>} />
        <Route path="system/role/config" element={<Suspense fallback={<PageLoading />}><RolePermConfig /></Suspense>} />
        <Route path="system/permission" element={<Suspense fallback={<PageLoading />}><PermissionList /></Suspense>} />
        <Route path="system/menu" element={<Suspense fallback={<PageLoading />}><MenuList /></Suspense>} />
        <Route path="system/dept" element={<Suspense fallback={<PageLoading />}><DeptList /></Suspense>} />
        <Route path="system/post" element={<Suspense fallback={<PageLoading />}><PostList /></Suspense>} />
        <Route path="system/dict" element={<Suspense fallback={<PageLoading />}><DictList /></Suspense>} />
        <Route path="profile" element={<Suspense fallback={<PageLoading />}><Profile /></Suspense>} />
        
        {/* 兼容旧路径 */}
        <Route path="roles" element={<Navigate to="/system/role" replace />} />
        <Route path="permissions" element={<Navigate to="/system/permission" replace />} />
      </Route>
    </Routes>
  );
}
