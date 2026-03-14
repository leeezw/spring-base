import { useState } from 'react';
import { Button, Modal, Form, Input, InputNumber, Select, Switch, Tag, Space, message, Drawer } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import ProTableV2 from '../components/ProTableV2.jsx';
import request from '../api/index.js';

export default function DictList() {
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [form] = Form.useForm();
  const [refreshKey, setRefreshKey] = useState(0);

  const columns = [
    { title: 'id', dataIndex: 'id' },
    { title: 'dictCode', dataIndex: 'dictCode' },
    { title: 'dictName', dataIndex: 'dictName' },
    { title: 'description', dataIndex: 'description' },
    { title: 'status', dataIndex: 'status', render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag> },
    { title: 'sortOrder', dataIndex: 'sortOrder' },
    { title: 'createTime', dataIndex: 'createTime' },
    { title: 'updateTime', dataIndex: 'updateTime' },
    {
      title: '操作', dataIndex: 'action', hideInSearch: true,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button>
        </Space>
      ),
    },
  ];

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue(record);
    setDrawerVisible(true);
  };

  const handleAdd = () => {
    setEditingRecord(null);
    form.resetFields();
    setDrawerVisible(true);
  };

  const handleSubmit = async (values) => {
    try {
      if (editingRecord) {
        await request.put('/dict', { ...values, id: editingRecord.id });
        message.success('更新成功');
      } else {
        await request.post('/dict', values);
        message.success('创建成功');
      }
      setDrawerVisible(false);
      setRefreshKey(k => k + 1);
    } catch (e) { message.error(e.message); }
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定删除吗？`,
      onOk: async () => {
        await request.delete(`/dict/${record.id}`);
        message.success('删除成功');
        setRefreshKey(k => k + 1);
      }
    });
  };

  return (
    <>
      <ProTableV2
        key={refreshKey}
        headerTitle="sys_dict"
        columns={columns}
        request={(params) => request.get('/dict/page', { params })}
        toolBarRender={() => [
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增</Button>
        ]}
      />

      <Drawer
        title={editingRecord ? '编辑' : '新增'}
        width={480}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type="primary" onClick={() => form.submit()}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="dictCode" label="dictCode" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="dictName" label="dictName" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="description">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="status">
            <Select options={[{value:1,label:'启用'},{value:0,label:'禁用'}]} />
          </Form.Item>
          <Form.Item name="sortOrder" label="sortOrder">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Drawer>
    </>
  );
}
