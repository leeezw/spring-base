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
import { IconPlus, IconEdit, IconDelete, IconSearch, IconRefresh } from '@arco-design/web-react/icon';
import axios from 'axios';

const { Option } = Select;

export default function UserManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();
  const [searchForm] = Form.useForm();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      width: 150,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      width: 200,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 150,
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

  const fetchData = async (pageNum = pagination.current, pageSize = pagination.pageSize) => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const res = await axios.get('/api/system/user/page', {
        params: {
          pageNum,
          pageSize,
          username: values.username || undefined,
        },
      });
      if (res.data.code === 200) {
        setData(res.data.data.records || []);
        setPagination({
          current: res.data.data.current,
          pageSize: res.data.data.size,
          total: res.data.data.total,
        });
      }
    } catch (error) {
      Message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSearch = () => {
    fetchData(1, pagination.pageSize);
  };

  const handleReset = () => {
    searchForm.resetFields();
    fetchData(1, pagination.pageSize);
  };

  const handleAdd = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      const res = await axios.delete(`/api/system/user/delete/${id}`);
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
        ? '/api/system/user/update'
        : '/api/system/user/save';
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

  const handlePageChange = (current, pageSize) => {
    fetchData(current, pageSize);
  };

  return (
    <div style={{ padding: '20px' }}>
      {/* 搜索区域 */}
      <Card style={{ marginBottom: 20 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item field="username" label="用户名">
            <Input
              placeholder="请输入用户名"
              prefix={<IconSearch />}
              allowClear
              style={{ width: 200 }}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<IconSearch />} onClick={handleSearch}>
                搜索
              </Button>
              <Button icon={<IconRefresh />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 表格区域 */}
      <Card
        title="用户列表"
        extra={
          <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
            新增用户
          </Button>
        }
      >
        <Table
          columns={columns}
          data={data}
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showTotal: true,
            sizeCanChange: true,
            onChange: handlePageChange,
          }}
          rowKey="id"
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingId ? '编辑用户' : '新增用户'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
        focusLock={true}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="用户名"
            field="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingId} />
          </Form.Item>

          <Form.Item label="昵称" field="nickname">
            <Input placeholder="请输入昵称" />
          </Form.Item>

          {!editingId && (
            <Form.Item
              label="密码"
              field="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}

          <Form.Item label="邮箱" field="email">
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item label="手机号" field="phone">
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item label="状态" field="status">
            <Select>
              <Option value={1}>正常</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
