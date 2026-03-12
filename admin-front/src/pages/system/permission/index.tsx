import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Message,
  Popconfirm,
  Tag,
} from '@arco-design/web-react';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-react/icon';
import axios from 'axios';

const { Option } = Select;

export default function PermissionManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();

  const columns = [
    {
      title: '权限名称',
      dataIndex: 'permissionName',
      width: 300,
    },
    {
      title: '权限编码',
      dataIndex: 'permissionCode',
      width: 250,
    },
    {
      title: '权限类型',
      dataIndex: 'permissionType',
      width: 120,
      render: (type) => {
        const typeMap = {
          1: { text: '菜单', color: 'blue' },
          2: { text: '按钮', color: 'green' },
          3: { text: 'API', color: 'orange' },
        };
        const config = typeMap[type] || { text: '未知', color: 'gray' };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '正常' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button
            type="text"
            size="small"
            icon={<IconEdit />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除吗？"
            onOk={() => handleDelete(record.id)}
          >
            <Button type="text" size="small" status="danger" icon={<IconDelete />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/system/permission/tree');
      if (res.data.code === 200) {
        setData(res.data.data || []);
      }
    } catch (error) {
      Message.error('获取权限列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAdd = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 1, permissionType: 1, sortOrder: 0 });
    setVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      const res = await axios.delete(`/api/system/permission/delete/${id}`);
      if (res.data.code === 200) {
        Message.success('删除成功');
        fetchData();
      } else {
        Message.error(res.data.message || '删除失败');
      }
    } catch (error) {
      Message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validate();
      const url = editingId
        ? '/api/system/permission/update'
        : '/api/system/permission/save';
      const method = editingId ? 'put' : 'post';
      const data = editingId ? { ...values, id: editingId } : values;

      const res = await axios[method](url, data);
      if (res.data.code === 200) {
        Message.success(editingId ? '更新成功' : '新增成功');
        setVisible(false);
        fetchData();
      } else {
        Message.error(res.data.message || '操作失败');
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  return (
    <Card
      title="权限管理"
      extra={
        <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
          新增权限
        </Button>
      }
    >
      <Table
        columns={columns}
        data={data}
        loading={loading}
        pagination={false}
        rowKey="id"
        defaultExpandAllRows
      />

      <Modal
        title={editingId ? '编辑权限' : '新增权限'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
        focusLock={true}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="权限名称"
            field="permissionName"
            rules={[{ required: true, message: '请输入权限名称' }]}
          >
            <Input placeholder="请输入权限名称" />
          </Form.Item>

          <Form.Item
            label="权限编码"
            field="permissionCode"
            rules={[{ required: true, message: '请输入权限编码' }]}
            disabled={!!editingId}
          >
            <Input placeholder="例如: system:user:add" disabled={!!editingId} />
          </Form.Item>

          <Form.Item
            label="权限类型"
            field="permissionType"
            rules={[{ required: true, message: '请选择权限类型' }]}
          >
            <Select placeholder="请选择权限类型">
              <Option value={1}>菜单</Option>
              <Option value={2}>按钮</Option>
              <Option value={3}>API</Option>
            </Select>
          </Form.Item>

          <Form.Item label="父级ID" field="parentId">
            <Input placeholder="0表示顶级权限" />
          </Form.Item>

          <Form.Item label="排序" field="sortOrder">
            <Input type="number" placeholder="数字越小越靠前" />
          </Form.Item>

          <Form.Item label="状态" field="status">
            <Select>
              <Option value={1}>正常</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>

          <Form.Item label="备注" field="remark">
            <Input.TextArea placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
