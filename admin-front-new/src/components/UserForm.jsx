import { useState, useEffect, useCallback } from 'react';
import { Form, Input, Select, TreeSelect, Divider, Spin } from 'antd';
import request from '../api/index.js';
import './UserForm.css';

const { Option } = Select;

/**
 * 用户表单组件
 * 用于新增和编辑用户——含部门选择、角色分配
 */
export default function UserForm({ form, initialValues, onFinish }) {
  const isEdit = !!initialValues;
  const [deptTree, setDeptTree] = useState([]);
  const [roleList, setRoleList] = useState([]);
  const [postList, setPostList] = useState([]);
  const [userRoleIds, setUserRoleIds] = useState([]);
  const [loading, setLoading] = useState(false);

  // 转换部门树为TreeSelect需要的格式
  const transformDeptTree = (nodes) => {
    if (!nodes) return [];
    return nodes.map(node => ({
      value: node.id,
      title: node.deptName,
      children: node.children ? transformDeptTree(node.children) : [],
    }));
  };

  // 加载部门树和角色列表
  const loadFormData = useCallback(async () => {
    setLoading(true);
    try {
      const [deptRes, roleRes, postRes] = await Promise.all([
        request.get('/system/dept/tree'),
        request.get('/system/role/list'),
        request.get('/system/post/list?status=1'),
      ]);
      if (deptRes.code === 200 && deptRes.data) {
        setDeptTree(transformDeptTree(deptRes.data));
      }
      if (roleRes.code === 200 && roleRes.data) {
        setRoleList(roleRes.data);
      }
      if (postRes.code === 200 && postRes.data) {
        setPostList(postRes.data);
      }

      // 编辑模式：加载用户已有的角色
      if (isEdit && initialValues?.id) {
        const relRes = await request.get(`/system/relation/user/${initialValues.id}/roles`);
        if (relRes.code === 200 && relRes.data) {
          setUserRoleIds(relRes.data);
          form.setFieldsValue({ roleIds: relRes.data });
        }
        const postRelRes = await request.get(`/system/relation/user/${initialValues.id}/posts`);
        if (postRelRes.code === 200 && postRelRes.data) {
          form.setFieldsValue({ postIds: postRelRes.data });
        }
      }
    } catch (e) {
      console.error('loadFormData error:', e);
    } finally {
      setLoading(false);
    }
  }, [isEdit, initialValues, form]);

  useEffect(() => {
    loadFormData();
  }, [loadFormData]);

  // 包装 onFinish，附带 roleIds
  const handleFinish = (values) => {
    onFinish?.({ ...values, roleIds: values.roleIds || [], postIds: values.postIds || [] });
  };

  return (
    <Spin spinning={loading}>
      <Form
        form={form}
        layout="vertical"
        initialValues={{ status: 1, deptId: null, roleIds: [], postIds: [], ...initialValues }}
        onFinish={handleFinish}
        className="user-form"
      >
        <Divider orientation="left" plain>基本信息</Divider>

        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, message: '用户名至少3个字符' },
            { max: 20, message: '用户名最多20个字符' },
          ]}
        >
          <Input placeholder="请输入用户名" disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="password"
          label="初始密码"
          rules={[
            { required: !isEdit, message: '请输入密码' },
            { min: 6, message: '密码至少6个字符' },
          ]}
        >
          <Input.Password placeholder={isEdit ? '留空则不修改密码' : '请输入密码'} />
        </Form.Item>

        <Form.Item name="nickname" label="昵称" rules={[{ max: 50, message: '最多50个字符' }]}>
          <Input placeholder="请输入昵称" />
        </Form.Item>

        <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '请输入有效邮箱' }]}>
          <Input placeholder="请输入邮箱" />
        </Form.Item>

        <Form.Item name="phone" label="手机号">
          <Input placeholder="请输入手机号" />
        </Form.Item>

        <Divider orientation="left" plain>组织与角色</Divider>

        <Form.Item name="deptId" label="所属部门">
          <TreeSelect
            treeData={deptTree}
            placeholder="请选择部门"
            allowClear
            treeDefaultExpandAll
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item name="roleIds" label="分配角色">
          <Select
            mode="multiple"
            placeholder="请选择角色"
            allowClear
            style={{ width: '100%' }}
          >
            {roleList.map(role => (
              <Option key={role.id} value={role.id} disabled={role.status !== 1}>
                {role.roleName}（{role.roleCode}）
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item name="postIds" label="岗位">
          <Select
            mode="multiple"
            placeholder="请选择岗位"
            allowClear
            style={{ width: '100%' }}
          >
            {postList.map(post => (
              <Option key={post.id} value={post.id}>
                {post.postName}{post.deptName ? ` — ${post.deptName}` : ''}
              </Option>
            ))}
          </Select>
        </Form.Item>

        {!isEdit && (
          <Form.Item name="status" label="状态">
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>
        )}
      </Form>
    </Spin>
  );
}
