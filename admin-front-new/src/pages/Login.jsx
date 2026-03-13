import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { RightOutlined, CheckCircleFilled, BuildOutlined } from '@ant-design/icons';
import request from '../api/index.js';
import { useAuthContext } from '../hooks/AuthProvider.jsx';
import './Login.css';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // 两阶段登录状态
  const [phase, setPhase] = useState('credentials'); // credentials | tenants | entering
  const [tenantList, setTenantList] = useState([]);
  const [selectedTenant, setSelectedTenant] = useState(null);
  const [enteringAnimation, setEnteringAnimation] = useState(false);
  
  const navigate = useNavigate();
  const { setToken, setUser } = useAuthContext();

  // 阶段1：提交用户名密码，获取租户列表
  const handleCredentialsSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await request.post('/auth/pre-login', { username, password });
      if (res.code !== 200 || !res.data || res.data.length === 0) {
        throw new Error(res.message || '用户名或密码错误');
      }
      
      setTenantList(res.data);
      
      // 如果只有一个租户，自动选中但仍展示
      if (res.data.length === 1) {
        setSelectedTenant(res.data[0]);
      }
      
      // 切换到租户选择阶段
      setPhase('tenants');
    } catch (err) {
      setError(err.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  // 阶段2：选择租户后完成登录
  const handleTenantSelect = async (tenant) => {
    setSelectedTenant(tenant);
    setEnteringAnimation(true);
    setError(null);
    
    try {
      const res = await request.post('/auth/login', {
        tenantCode: tenant.tenantCode,
        username,
        password,
      });
      
      if (res.code !== 200) {
        throw new Error(res.message || '登录失败');
      }
      
      const data = res.data || {};
      const token = data.token;
      if (!token) throw new Error('登录响应异常');
      
      const userInfo = data.userInfo || {};
      setToken(token);
      setUser({
        id: userInfo.userId,
        username: userInfo.username,
        nickname: userInfo.nickname,
        avatar: userInfo.avatar,
        permissions: userInfo.permissions,
        roles: userInfo.roles,
        tenantId: userInfo.tenantId,
      });
      
      if (rememberMe) {
        localStorage.setItem('uc_remember', 'true');
      }
      
      // 进入动画
      setPhase('entering');
      setTimeout(() => {
        navigate('/', { replace: true });
      }, 1200);
      
    } catch (err) {
      setEnteringAnimation(false);
      setError(err.message || '登录失败');
    }
  };

  // 返回到凭证输入
  const handleBackToCredentials = () => {
    setPhase('credentials');
    setTenantList([]);
    setSelectedTenant(null);
    setError(null);
  };

  return (
    <div className="login-page">
      <div className={`login-form-container ${phase !== 'credentials' ? 'phase-tenant' : ''} ${phase === 'entering' ? 'phase-entering' : ''}`}>
        
        {/* 阶段1：凭证输入 */}
        {phase === 'credentials' && (
          <>
            <h1 className="form-title">登录用户中心</h1>
            <form className="login-form" onSubmit={handleCredentialsSubmit}>
              {error && (
                <div className="login-error">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <circle cx="8" cy="8" r="7" fill="rgba(125, 40, 40, 1)"/>
                    <path d="M8 4V8M8 12H8.01" stroke="white" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  <span>{error}</span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">用户名</label>
                <div className="input-wrapper">
                  <input
                    type="text"
                    className="form-input"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="请输入用户名"
                    required
                    autoComplete="username"
                    autoFocus
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">密码</label>
                <div className="password-input-wrapper">
                  <input
                    type={showPassword ? "text" : "password"}
                    className="form-input password-input"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="请输入密码"
                    required
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 3.75C5.83 3.75 2.275 6.342 .833 10 2.275 13.658 5.833 16.25 10 16.25s7.725-2.592 9.167-6.25C17.725 6.342 14.167 3.75 10 3.75zm0 10.417A4.17 4.17 0 015.833 10 4.17 4.17 0 0110 5.833 4.17 4.17 0 0114.167 10 4.17 4.17 0 0110 14.167zM10 7.5A2.503 2.503 0 007.5 10c0 1.383 1.117 2.5 2.5 2.5s2.5-1.117 2.5-2.5S11.383 7.5 10 7.5z" fill="#7d8592"/>
                        <path d="M1.667 1.667L18.333 18.333" stroke="#7d8592" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                    ) : (
                      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 3.75C5.83 3.75 2.275 6.342.833 10 2.275 13.658 5.833 16.25 10 16.25s7.725-2.592 9.167-6.25C17.725 6.342 14.167 3.75 10 3.75zm0 10.417A4.17 4.17 0 015.833 10 4.17 4.17 0 0110 5.833 4.17 4.17 0 0114.167 10 4.17 4.17 0 0110 14.167zM10 7.5A2.503 2.503 0 007.5 10c0 1.383 1.117 2.5 2.5 2.5s2.5-1.117 2.5-2.5S11.383 7.5 10 7.5z" fill="#7d8592"/>
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              <div className="form-options">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="checkbox-input"
                  />
                  <span className="checkbox-text">记住我</span>
                </label>
              </div>

              <button type="submit" className="login-button" disabled={loading}>
                {loading ? (
                  <>
                    <svg className="spinner" width="16" height="16" viewBox="0 0 16 16">
                      <circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="2" fill="none" strokeDasharray="37.7" strokeDashoffset="9.4">
                        <animate attributeName="stroke-dasharray" values="37.7,37.7;18.85,56.55;37.7,37.7" dur="1s" repeatCount="indefinite"/>
                        <animate attributeName="stroke-dashoffset" values="0;-18.85;0" dur="1s" repeatCount="indefinite"/>
                      </circle>
                    </svg>
                    验证中...
                  </>
                ) : (
                  <>
                    <span>下一步</span>
                    <RightOutlined className="button-arrow" />
                  </>
                )}
              </button>
            </form>
          </>
        )}

        {/* 阶段2：租户选择 */}
        {phase === 'tenants' && (
          <div className="tenant-phase">
            <div className="tenant-header">
              <div className="tenant-check-icon">
                <CheckCircleFilled />
              </div>
              <h2 className="tenant-title">身份验证成功</h2>
              <p className="tenant-subtitle">
                你好，{tenantList[0]?.nickname || username}。请选择要进入的租户空间
              </p>
            </div>

            {error && (
              <div className="login-error">
                <span>{error}</span>
              </div>
            )}

            <div className="tenant-list">
              {tenantList.map((tenant, index) => (
                <button
                  key={tenant.tenantId}
                  className={`tenant-card ${selectedTenant?.tenantId === tenant.tenantId ? 'tenant-card-selected' : ''} ${enteringAnimation ? 'tenant-card-entering' : ''}`}
                  style={{ animationDelay: `${index * 0.1}s` }}
                  onClick={() => !enteringAnimation && handleTenantSelect(tenant)}
                  disabled={enteringAnimation}
                >
                  <div className="tenant-card-icon">
                    <BuildOutlined />
                  </div>
                  <div className="tenant-card-info">
                    <div className="tenant-card-name">{tenant.tenantName}</div>
                    <div className="tenant-card-code">{tenant.tenantCode}</div>
                  </div>
                  <div className="tenant-card-arrow">
                    <RightOutlined />
                  </div>
                </button>
              ))}
            </div>

            <button className="back-button" onClick={handleBackToCredentials} disabled={enteringAnimation}>
              返回重新登录
            </button>
          </div>
        )}

        {/* 阶段3：进入动画 */}
        {phase === 'entering' && (
          <div className="entering-phase">
            <div className="entering-spinner">
              <svg width="48" height="48" viewBox="0 0 48 48">
                <circle cx="24" cy="24" r="20" stroke="var(--primary-color)" strokeWidth="3" fill="none" strokeDasharray="126" strokeDashoffset="30" strokeLinecap="round">
                  <animateTransform attributeName="transform" type="rotate" values="0 24 24;360 24 24" dur="1s" repeatCount="indefinite"/>
                </circle>
              </svg>
            </div>
            <h2 className="entering-title">正在进入 {selectedTenant?.tenantName}</h2>
            <p className="entering-subtitle">正在加载工作空间...</p>
          </div>
        )}
      </div>
    </div>
  );
}
