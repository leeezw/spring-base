import { LockOutlined, UserOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/cert';
import { TOKEN_KEY } from '../api/request';

export default function Login() {
  const navigate = useNavigate();

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg,#e6f0ff 0%,#f5f7fa 100%)',
      }}
    >
      <div style={{ width: 360, background: '#fff', padding: 24, borderRadius: 12, boxShadow: '0 6px 24px rgba(0,0,0,0.08)' }}>
        <LoginForm
          title="证书自动化管理"
          subTitle="SSL 证书签发 / 部署 / 续期一站式"
          logo={<SafetyCertificateOutlined style={{ color: '#1677ff' }} />}
          onFinish={async (values) => {
            try {
              const res = await login(values as { username: string; password: string });
              localStorage.setItem(TOKEN_KEY, res.token);
              message.success('登录成功');
              navigate('/dashboard');
              return true;
            } catch {
              return false;
            }
          }}
        >
          <ProFormText
            name="username"
            fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
            placeholder="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          />
          <ProFormText.Password
            name="password"
            fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
            placeholder="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          />
        </LoginForm>
      </div>
    </div>
  );
}
