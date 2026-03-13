import { useState, useEffect, useCallback } from 'react';
import { Card, Form, Input, Button, message, Tabs, Descriptions, Tag, Space, Avatar, Divider, Row, Col, Spin } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined, EditOutlined, SafetyOutlined, IdcardOutlined } from '@ant-design/icons';
import request from '../api/index.js';
import { useAuthContext } from '../hooks/AuthProvider.jsx';
import './Profile.css';

export default function Profile() {
  const { user: authUser } = useAuthContext();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [profileForm] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/auth/profile');
      if (res.code === 200) {
        setProfile(res.data);
        profileForm.setFieldsValue({
          nickname: res.data.user?.nickname || '',
          email: res.data.user?.email || '',
          phone: res.data.user?.phone || '',
        });
      }
    } catch (e) { message.error('加载失败'); }
    finally { setLoading(false); }
  }, [profileForm]);

  useEffect(() => { loadProfile(); }, [loadProfile]);

  const handleUpdateProfile = async (values) => {
    setProfileSaving(true);
    try {
      const res = await request.put('/auth/profile', values);
      if (res.code === 200) {
        message.success('个人信息已更新');
        loadProfile();
      } else {
        message.error(res.message);
      }
    } catch (e) { message.error('更新失败'); }
    finally { setProfileSaving(false); }
  };

  const handleChangePassword = async (values) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次密码输入不一致');
      return;
    }
    setPasswordSaving(true);
    try {
      const res = await request.put('/auth/password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      if (res.code === 200) {
        message.success('密码修改成功');
        passwordForm.resetFields();
      } else {
        message.error(res.message);
      }
    } catch (e) { message.error('修改失败'); }
    finally { setPasswordSaving(false); }
  };

  const user = profile?.user;

  return (
    <div className="profile-page">
      <Spin spinning={loading}>
        <Row gutter={16}>
          {/* 左侧：用户卡片 */}
          <Col xs={24} md={8}>
            <Card className="profile-card">
              <div className="profile-avatar-section">
                <Avatar size={80} icon={<UserOutlined />} src={user?.avatar} className="profile-avatar" />
                <h2 className="profile-name">{user?.nickname || user?.username || '-'}</h2>
                <p className="profile-username">@{user?.username}</p>
              </div>

              <Divider style={{ margin: '16px 0' }} />

              <div className="profile-info-list">
                <div className="profile-info-item">
                  <MailOutlined className="profile-info-icon" />
                  <span>{user?.email || '未设置'}</span>
                </div>
                <div className="profile-info-item">
                  <PhoneOutlined className="profile-info-icon" />
                  <span>{user?.phone || '未设置'}</span>
                </div>
                <div className="profile-info-item">
                  <SafetyOutlined className="profile-info-icon" />
                  <Space size={[4, 4]} wrap>
                    {(profile?.roles || []).map((r, i) => (
                      <Tag key={i} color="blue">{typeof r === 'string' ? r : r.roleName || r}</Tag>
                    ))}
                    {(!profile?.roles || profile.roles.length === 0) && <Tag>未分配角色</Tag>}
                  </Space>
                </div>
              </div>

              <Divider style={{ margin: '16px 0' }} />

              <Descriptions column={1} size="small" labelStyle={{ color: '#8c8c8c', width: 80 }}>
                <Descriptions.Item label="部门">{user?.deptName || '-'}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={user?.status === 1 ? 'success' : 'default'}>{user?.status === 1 ? '正常' : '禁用'}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="注册时间">{user?.createTime?.split('T')[0] || '-'}</Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>

          {/* 右侧：编辑表单 */}
          <Col xs={24} md={16}>
            <Card className="profile-edit-card">
              <Tabs items={[
                {
                  key: 'info',
                  label: <span><EditOutlined /> 修改信息</span>,
                  children: (
                    <Form form={profileForm} layout="vertical" onFinish={handleUpdateProfile} className="profile-form">
                      <Form.Item name="nickname" label="昵称" rules={[{ max: 50, message: '最多50个字符' }]}>
                        <Input prefix={<UserOutlined />} placeholder="请输入昵称" />
                      </Form.Item>
                      <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '请输入有效邮箱' }]}>
                        <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                      </Form.Item>
                      <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1\d{10}$/, message: '请输入有效手机号' }]}>
                        <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
                      </Form.Item>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" loading={profileSaving} icon={<EditOutlined />}>
                          保存修改
                        </Button>
                      </Form.Item>
                    </Form>
                  ),
                },
                {
                  key: 'password',
                  label: <span><LockOutlined /> 修改密码</span>,
                  children: (
                    <Form form={passwordForm} layout="vertical" onFinish={handleChangePassword} className="profile-form">
                      <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="请输入原密码" />
                      </Form.Item>
                      <Form.Item name="newPassword" label="新密码" rules={[
                        { required: true, message: '请输入新密码' },
                        { min: 6, message: '密码至少6个字符' },
                      ]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="请输入新密码" />
                      </Form.Item>
                      <Form.Item name="confirmPassword" label="确认新密码" rules={[
                        { required: true, message: '请确认新密码' },
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                            return Promise.reject(new Error('两次密码输入不一致'));
                          },
                        }),
                      ]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="请再次输入新密码" />
                      </Form.Item>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" loading={passwordSaving} icon={<LockOutlined />}>
                          修改密码
                        </Button>
                      </Form.Item>
                    </Form>
                  ),
                },
              ]} />
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  );
}
