import { HashRouter, Navigate, Route, Routes } from 'react-router-dom';
import { TOKEN_KEY } from './api/request';
import BasicLayout from './layouts/BasicLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Certificates from './pages/Certificates';
import AcmeAccounts from './pages/AcmeAccounts';
import DnsProviders from './pages/DnsProviders';
import DeployTargets from './pages/DeployTargets';
import TaskLogs from './pages/TaskLogs';

function RequireAuth({ children }: { children: JSX.Element }) {
  const token = localStorage.getItem(TOKEN_KEY);
  return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <HashRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <BasicLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="certificates" element={<Certificates />} />
          <Route path="accounts" element={<AcmeAccounts />} />
          <Route path="dns" element={<DnsProviders />} />
          <Route path="targets" element={<DeployTargets />} />
          <Route path="logs" element={<TaskLogs />} />
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </HashRouter>
  );
}
