import {
  Form,
  Input,
  Checkbox,
  Link,
  Button,
  Space,
  Select,
  Message,
} from '@arco-design/web-react';
import { FormInstance } from '@arco-design/web-react/es/Form';
import { IconLock, IconUser } from '@arco-design/web-react/icon';
import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import useStorage from '@/utils/useStorage';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './style/index.module.less';

const Option = Select.Option;

export default function LoginForm() {
  const formRef = useRef<FormInstance>();
  const [errorMessage, setErrorMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [loginParams, setLoginParams, removeLoginParams] =
    useStorage('loginParams');

  const t = useLocale(locale);

  const [rememberPassword, setRememberPassword] = useState(!!loginParams);

  function afterLoginSuccess(token: string, userInfo: any) {
    // 保存Token
    localStorage.setItem('token', token);
    // 保存用户信息
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    // 记录登录状态
    localStorage.setItem('userStatus', 'login');
    
    // 记住密码
    if (rememberPassword) {
      const params = formRef.current.getFieldsValue();
      setLoginParams(JSON.stringify(params));
    } else {
      removeLoginParams();
    }
    
    Message.success('登录成功');
    
    // 跳转首页
    setTimeout(() => {
      window.location.href = '/';
    }, 500);
  }

  function login(params) {
    setErrorMessage('');
    setLoading(true);
    
    axios
      .post('/api/auth/login', {
        tenantCode: params.tenantCode,
        username: params.userName,
        password: params.password,
      })
      .then((res) => {
        const { code, data, message } = res.data;
        if (code === 200) {
          afterLoginSuccess(data.token, data.userInfo);
        } else {
          setErrorMessage(message || '登录失败');
        }
      })
      .catch((error) => {
        setErrorMessage(error.response?.data?.message || '网络错误，请稍后重试');
      })
      .finally(() => {
        setLoading(false);
      });
  }

  function onSubmitClick() {
    formRef.current.validate().then((values) => {
      login(values);
    });
  }

  // 读取 localStorage，设置初始值
  useEffect(() => {
    const rememberPassword = !!loginParams;
    setRememberPassword(rememberPassword);
    if (formRef.current && rememberPassword) {
      const parseParams = JSON.parse(loginParams);
      formRef.current.setFieldsValue(parseParams);
    }
  }, [loginParams]);

  return (
    <div className={styles['login-form-wrapper']}>
      <div className={styles['login-form-title']}>🌰 Scaffold</div>
      <div className={styles['login-form-sub-title']}>
        企业级多租户SaaS脚手架
      </div>
      <div className={styles['login-form-error-msg']}>{errorMessage}</div>
      <Form
        className={styles['login-form']}
        layout="vertical"
        ref={formRef}
        initialValues={{ 
          tenantCode: 'default',
          userName: 'admin', 
          password: 'admin123' 
        }}
      >
        <Form.Item
          field="tenantCode"
          rules={[{ required: true, message: '请选择租户' }]}
        >
          <Select placeholder="请选择租户">
            <Option value="platform">平台管理</Option>
            <Option value="default">默认租户</Option>
            <Option value="test002">测试租户002</Option>
          </Select>
        </Form.Item>
        
        <Form.Item
          field="userName"
          rules={[{ required: true, message: t['login.form.userName.errMsg'] }]}
        >
          <Input
            prefix={<IconUser />}
            placeholder={t['login.form.userName.placeholder']}
            onPressEnter={onSubmitClick}
          />
        </Form.Item>
        
        <Form.Item
          field="password"
          rules={[{ required: true, message: t['login.form.password.errMsg'] }]}
        >
          <Input.Password
            prefix={<IconLock />}
            placeholder={t['login.form.password.placeholder']}
            onPressEnter={onSubmitClick}
          />
        </Form.Item>
        
        <Space size={16} direction="vertical">
          <div className={styles['login-form-password-actions']}>
            <Checkbox checked={rememberPassword} onChange={setRememberPassword}>
              {t['login.form.rememberPassword']}
            </Checkbox>
          </div>
          <Button type="primary" long onClick={onSubmitClick} loading={loading}>
            {t['login.form.login']}
          </Button>
        </Space>
      </Form>
      
      <div style={{ marginTop: 16, fontSize: 12, color: '#86909c', textAlign: 'center' }}>
        <div>平台管理: platform_admin / admin123</div>
        <div>默认租户: admin / admin123</div>
        <div>测试租户: test_user / admin123</div>
      </div>
    </div>
  );
}
