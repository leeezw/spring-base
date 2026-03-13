import { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Avatar, Upload, message, Divider, Space, Tag, Descriptions, Modal } from 'antd';
import { 
  UserOutlined,
  EditOutlined,
  CameraOutlined,
  LockOutlined,
  MailOutlined,
  PhoneOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { useAuthContext } from '../hooks/AuthProvider.jsx';
import request from '../api/index.js';
import FormField from '../components/FormField.jsx';
import './Profile.css';

const { TextArea } = Input;

export default function Profile() {
  const { user, setUser } = useAuthContext();
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        nickname: user.nickname,
        email: user.email,
        phone: user.phone,
        avatar: user.avatar,
        remark: user.remark,
      });
    }
  }, [user, form]);

  // 更新个人信息
  const handleUpdateProfile = async (values) => {
    setLoading(true);
    try {
      const res = await request.put('/system/user', { ...values, id: user.id });
      if (res.code === 200) {
        message.success('个人信息更新成功');
        setUser({ ...user, ...values });
        setEditMode(false);
      } else {
        message.error(res.message || '更新失败');
      }
    } catch (error) {
      message.error(error.message || '更新失败');
    } finally {
      setLoading(false);
    }
  };

  // 修改密码
  const handleChangePassword = async (values) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }
    
    setPasswordLoading(true);
    try {
      const res = await request.put('/auth/password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      if (res.code === 200) {
        message.success('密码修改成功，请重新登录');
        setPasswordModalVisible(false);
        passwordForm.resetFields();
        // 可以在这里触发登出
      } else {
        message.error(res.message || '密码修改失败');
      }
    } catch (error) {
      message.error(error.message || '密码修改失败');
    } finally {
      setPasswordLoading(false);
    }
  };

  // 上传头像
  const handleAvatarChange = async (info) => {
    if (info.file.status === 'done') {
      const avatarUrl = info.file.response?.data?.url || info.file.response?.url;
      if (avatarUrl) {
        try {
          const res = await request.put('/system/user', { id: user.id, avatar: avatarUrl });
          if (res.code === 200) {
            setUser({ ...user, avatar: avatarUrl });
            message.success('头像更新成功');
          }
        } catch (error) {
          message.error('头像更新失败');
        }
      }
    }
  };

  const getAvatarProps = () => {
    if (user?.avatar && typeof user.avatar === 'string' && user.avatar.trim() !== '') {
      return { src: user.avatar };
    }
    const name = user?.nickname || user?.username || '';
    if (name) {
      return { 
        style: { backgroundColor: '#3f8cff', color: 'white' },
        children: name.charAt(0).toUpperCase()
      };
    }
    return { 
      icon: <UserOutlined />,
      style: { backgroundColor: '#3f8cff', color: 'white' }
    };
  };

  return (
    <div className="profile-page">
      <div className="profile-header">
        <h2>个人中心</h2>
        <p className="profile-description">管理您的个人信息和账户设置</p>
      </div>

      <div className="profile-content">
        {/* 基本信息卡片 */}
        <Card className="profile-card" title="基本信息">
          <div className="profile-avatar-section">
            <Upload
              name="avatar"
              action="/api/upload/avatar"
              showUploadList={false}
              onChange={handleAvatarChange}
              accept="image/*"
              disabled={!editMode}
            >
              <Avatar
                size={120}
                {...getAvatarProps()}
                className="profile-avatar"
              >
                {editMode && (
                  <div className="avatar-overlay">
                    <CameraOutlined />
                    <span>更换头像</span>
                  </div>
                )}
              </Avatar>
            </Upload>
            <div className="avatar-info">
              <h3>{user?.nickname || user?.username || '用户'}</h3>
              <p className="username-text">@{user?.username}</p>
              <Tag color="success" className="status-tag">已启用</Tag>
            </div>
          </div>

          <Divider />

          {!editMode ? (
            <Descriptions column={2} bordered className="profile-descriptions">
              <Descriptions.Item label="用户名">
                {user?.username || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="昵称">
                {user?.nickname || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="邮箱">
                {user?.email || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="手机号">
                {user?.phone || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {user?.remark || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {user?.createTime ? new Date(user.createTime).toLocaleString() : '-'}
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <Form
              form={form}
              layout="vertical"
              onFinish={handleUpdateProfile}
              className="profile-form"
            >
              <FormField
                name="username"
                label="用户名"
                required
              >
                <Input disabled prefix={<UserOutlined />} />
              </FormField>

              <FormField
                name="nickname"
                label="昵称"
                rules={[{ max: 50, message: '昵称最多50个字符' }]}
              >
                <Input placeholder="请输入昵称" />
              </FormField>

              <FormField
                name="email"
                label="邮箱"
                rules={[
                  { type: 'email', message: '请输入有效的邮箱地址' },
                ]}
              >
                <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
              </FormField>

              <FormField
                name="phone"
                label="手机号"
                rules={[
                  { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' },
                ]}
              >
                <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
              </FormField>

              <FormField
                name="remark"
                label="备注"
                rules={[{ max: 200, message: '备注最多200个字符' }]}
              >
                <TextArea rows={4} placeholder="请输入备注信息" />
              </FormField>

              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                  >
                    保存
                  </Button>
                  <Button onClick={() => {
                    form.resetFields();
                    setEditMode(false);
                  }}>
                    取消
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          )}

          {!editMode && (
            <div className="profile-actions">
              <Button
                type="primary"
                icon={<EditOutlined />}
                onClick={() => setEditMode(true)}
              >
                编辑信息
              </Button>
              <Button
                icon={<LockOutlined />}
                onClick={() => setPasswordModalVisible(true)}
              >
                修改密码
              </Button>
            </div>
          )}
        </Card>

        {/* 账户安全卡片 */}
        <Card className="profile-card" title="账户安全">
          <div className="security-item">
            <div className="security-info">
              <SafetyOutlined className="security-icon" />
              <div>
                <h4>登录密码</h4>
                <p>定期更换密码可以保护账户安全</p>
              </div>
            </div>
            <Button
              icon={<LockOutlined />}
              onClick={() => setPasswordModalVisible(true)}
            >
              修改密码
            </Button>
          </div>
        </Card>
      </div>

      {/* 修改密码弹窗 */}
      <Modal
        title="修改密码"
        open={passwordModalVisible}
        onCancel={() => {
          setPasswordModalVisible(false);
          passwordForm.resetFields();
        }}
        footer={null}
        width={500}
        className="password-modal"
        centered
        destroyOnHidden
      >
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={handleChangePassword}
          className="password-form"
        >
          <FormField
            name="oldPassword"
            label="当前密码"
            rules={[{ required: true, message: '请输入当前密码' }]}
            required
          >
            <Input.Password placeholder="请输入当前密码" />
          </FormField>

          <FormField
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少6个字符' },
            ]}
            required
          >
            <Input.Password placeholder="请输入新密码（至少6个字符）" />
          </FormField>

          <FormField
            name="confirmPassword"
            label="确认新密码"
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
            required
          >
            <Input.Password placeholder="请再次输入新密码" />
          </FormField>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={passwordLoading}
              >
                确认修改
              </Button>
              <Button onClick={() => {
                setPasswordModalVisible(false);
                passwordForm.resetFields();
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

