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

export default function DeptManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();

  const columns = [
    {
      title: '部门名称',
      dataIndex: 'deptName',
      width: 250,
    },
    {
      title: '负责人ID',
      dataIndex: 'leaderId',
      width: 120,
    },
    {
      title: '联系电话',
      dataIndex: 'phone',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      width: 200,
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
      fixed: 'right',
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
      const res = await axios.get('/api/system/dept/tree');
      if (res.data.code === 200) {
        setData(res.data.data || []);
      }
    } catch (error) {
      Message.error('获取部门列表失败');
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
    form.setFieldsValue({ status: 1, sortOrder: 0, parentId: 0 });
    setVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      const res = await axios.delete(`/api/system/dept/delete/${id}`);
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
        ? '/api/system/dept/update'
        : '/api/system/dept/save';
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
      title="部门管理"
      extra={
        <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
          新增部门
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
        scroll={{ x: 1200 }}
      />

      <Modal
        title={editingId ? '编辑部门' : '新增部门'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
        focusLock={true}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="部门名称"
            field="deptName"
            rules={[{ required: true, message: '请输入部门名称' }]}
          >
            <Input placeholder="请输入部门名称" />
          </Form.Item>

          <Form.Item label="负责人ID" field="leaderId">
            <Input type="number" placeholder="请输入负责人用户ID" />
          </Form.Item>

          <Form.Item label="联系电话" field="phone">
            <Input placeholder="请输入联系电话" />
          </Form.Item>

          <Form.Item label="邮箱" field="email">
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item label="父级部门ID" field="parentId">
            <Input type="number" placeholder="0表示顶级部门" />
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
        </Form>
      </Modal>
    </Card>
  );
}
