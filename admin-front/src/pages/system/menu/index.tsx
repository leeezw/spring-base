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

export default function MenuManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();

  const columns = [
    {
      title: '菜单名称',
      dataIndex: 'menuName',
      width: 250,
    },
    {
      title: '路径',
      dataIndex: 'path',
      width: 200,
    },
    {
      title: '组件',
      dataIndex: 'component',
      width: 200,
    },
    {
      title: '图标',
      dataIndex: 'icon',
      width: 150,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 80,
    },
    {
      title: '可见',
      dataIndex: 'visible',
      width: 80,
      render: (visible) => (
        <Tag color={visible === 1 ? 'green' : 'gray'}>
          {visible === 1 ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
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
      const res = await axios.get('/api/system/menu/tree');
      if (res.data.code === 200) {
        setData(res.data.data || []);
      }
    } catch (error) {
      Message.error('获取菜单列表失败');
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
    form.setFieldsValue({ status: 1, visible: 1, sortOrder: 0, parentId: 0 });
    setVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      const res = await axios.delete(`/api/system/menu/delete/${id}`);
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
        ? '/api/system/menu/update'
        : '/api/system/menu/save';
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
      title="菜单管理"
      extra={
        <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
          新增菜单
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
        scroll={{ x: 1400 }}
      />

      <Modal
        title={editingId ? '编辑菜单' : '新增菜单'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
        focusLock={true}
        style={{ width: 600 }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="菜单名称"
            field="menuName"
            rules={[{ required: true, message: '请输入菜单名称' }]}
          >
            <Input placeholder="请输入菜单名称" />
          </Form.Item>

          <Form.Item
            label="菜单路径"
            field="path"
            rules={[{ required: true, message: '请输入菜单路径' }]}
          >
            <Input placeholder="例如: /system/user" />
          </Form.Item>

          <Form.Item label="组件路径" field="component">
            <Input placeholder="例如: system/user/index" />
          </Form.Item>

          <Form.Item label="菜单图标" field="icon">
            <Input placeholder="例如: IconUser" />
          </Form.Item>

          <Form.Item label="父级ID" field="parentId">
            <Input type="number" placeholder="0表示顶级菜单" />
          </Form.Item>

          <Form.Item label="排序" field="sortOrder">
            <Input type="number" placeholder="数字越小越靠前" />
          </Form.Item>

          <Form.Item label="是否可见" field="visible">
            <Select>
              <Option value={1}>是</Option>
              <Option value={0}>否</Option>
            </Select>
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
