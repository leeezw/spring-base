import { useState, useRef } from 'react';
import { Button, Space, Tag, Modal, Form, Input, InputNumber, DatePicker, message, Drawer, Descriptions, Badge, Statistic, Card, Row, Col } from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, StopOutlined, CheckCircleOutlined,
  TeamOutlined, SafetyCertificateOutlined,
} from '@ant-design/icons';
import ProTableV2 from '../components/ProTableV2.jsx';
import request from '../api/index.js';
import { usePermission } from '../hooks/usePermission.jsx';
import dayjs from 'dayjs';

export default function TenantList() {
  const { hasPermission } = usePermission();
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailRecord, setDetailRecord] = useState(null);
  const [form] = Form.useForm();
  const tableRef = useRef();

  const handleAdd = () => { setEditingRecord(null); form.resetFields(); setDrawerVisible(true); };

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue({
      ...record,
      expireTime: record.expireTime ? dayjs(record.expireTime) : null,
    });
    setDrawerVisible(true);
  };

  const handleDetail = async (record) => {
    try {
      const res = await request.get(`/system/tenant/${record.id}`);
      if (res.code === 200) { setDetailRecord(res.data); setDetailVisible(true); }
    } catch (e) { message.error('加载失败'); }
  };

  const handleSubmit = async (values) => {
    const payload = {
      ...values,
      expireTime: values.expireTime?.format('YYYY-MM-DD HH:mm:ss'),
    };
    try {
      if (editingRecord) {
        payload.id = editingRecord.id;
        const res = await request.put('/system/tenant', payload);
        if (res.code === 200) { message.success('更新成功'); setDrawerVisible(false); tableRef.current?.reload?.(); }
        else message.error(res.message);
      } else {
        const res = await request.post('/system/tenant', payload);
        if (res.code === 200) { message.success('创建成功'); setDrawerVisible(false); tableRef.current?.reload?.(); }
        else message.error(res.message);
      }
    } catch (e) { message.error(e.message); }
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除', content: `删除租户「${record.tenantName}」？此操作不可恢复！`,
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/system/tenant/${record.id}`);
        if (res.code === 200) { message.success('删除成功'); tableRef.current?.reload?.(); }
        else message.error(res.message);
      }
    });
  };

  const handleToggleStatus = (record) => {
    const newStatus = record.status === 1 ? 0 : 1;
    Modal.confirm({
      title: newStatus === 1 ? '启用租户' : '停用租户',
      content: `确定${newStatus === 1 ? '启用' : '停用'}租户「${record.tenantName}」？`,
      onOk: async () => {
        const res = await request.put('/system/tenant', { id: record.id, status: newStatus });
        if (res.code === 200) { message.success('操作成功'); tableRef.current?.reload?.(); }
        else message.error(res.message);
      }
    });
  };

  const columns = [
    { title: '租户编码', dataIndex: 'tenantCode', width: 120, render: (v) => <code>{v}</code> },
    { title: '租户名称', dataIndex: 'tenantName', width: 150 },
    { title: '联系人', dataIndex: 'contactName', width: 100, hideInSearch: true },
    { title: '手机号', dataIndex: 'contactPhone', width: 130, hideInSearch: true },
    { title: '账号额度', dataIndex: 'accountCount', width: 90, hideInSearch: true,
      render: (v) => v === -1 ? <Tag color="gold">不限</Tag> : v
    },
    { title: '到期时间', dataIndex: 'expireTime', width: 120, hideInSearch: true,
      render: (v) => {
        if (!v) return <Tag color="gold">永久</Tag>;
        const isExpired = dayjs(v).isBefore(dayjs());
        return <Tag color={isExpired ? 'red' : 'green'}>{dayjs(v).format('YYYY-MM-DD')}</Tag>;
      }
    },
    { title: '状态', dataIndex: 'status', width: 80,
      render: (v) => v === 1 ? <Badge status="success" text="正常" /> : <Badge status="error" text="停用" />
    },
    { title: '创建时间', dataIndex: 'createTime', width: 170, hideInSearch: true,
      render: (v) => v?.replace('T', ' ')?.substring(0, 19)
    },
    {
      title: '操作', width: 200, hideInSearch: true,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handleDetail(record)}>详情</Button>
          {hasPermission('system:tenant:edit') && <Button size="small" type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>}
          {hasPermission('system:tenant:edit') && <Button size="small" type="link" icon={record.status === 1 ? <StopOutlined /> : <CheckCircleOutlined />}
            onClick={() => handleToggleStatus(record)}>
            {record.status === 1 ? '停用' : '启用'}
          </Button>}
          {record.id !== 1 && hasPermission('system:tenant:delete') && <Button size="small" type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button>}
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTableV2
        ref={tableRef}
        headerTitle="租户管理"
        columns={columns}
        request={(params) => request.get('/system/tenant/page', { params })}
        rowKey="id"
        toolBarRender={() => [
          hasPermission('system:tenant:add') && <Button key="add" type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增租户</Button>
        ]}
      />

      <Drawer
        title={editingRecord ? '编辑租户' : '新增租户'}
        width={480} open={drawerVisible} onClose={() => setDrawerVisible(false)} destroyOnClose
        extra={<Space><Button onClick={() => setDrawerVisible(false)}>取消</Button><Button type="primary" onClick={() => form.submit()}>保存</Button></Space>}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="tenantCode" label="租户编码" rules={[{ required: true }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '字母开头，只能包含字母数字下划线' }]}>
            <Input disabled={!!editingRecord} placeholder="例: company_a" />
          </Form.Item>
          <Form.Item name="tenantName" label="租户名称" rules={[{ required: true }]}>
            <Input placeholder="例: A公司" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="contactName" label="联系人"><Input /></Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPhone" label="联系电话"><Input /></Form.Item>
            </Col>
          </Row>
          <Form.Item name="contactEmail" label="联系邮箱"><Input /></Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="accountCount" label="账号额度" tooltip="-1表示不限">
                <InputNumber style={{ width: '100%' }} min={-1} placeholder="-1不限" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="expireTime" label="到期时间" tooltip="不填则永久有效">
                <DatePicker style={{ width: '100%' }} showTime />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Drawer>

      <Drawer title="租户详情" width={500} open={detailVisible} onClose={() => setDetailVisible(false)} destroyOnClose>
        {detailRecord && (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={12}><Card><Statistic title="账号额度" value={detailRecord.accountCount === -1 ? '不限' : detailRecord.accountCount} prefix={<TeamOutlined />} /></Card></Col>
              <Col span={12}><Card><Statistic title="状态" value={detailRecord.status === 1 ? '正常' : '停用'}
                prefix={<SafetyCertificateOutlined />} valueStyle={{ color: detailRecord.status === 1 ? '#52c41a' : '#ff4d4f' }} /></Card></Col>
            </Row>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="租户编码"><code>{detailRecord.tenantCode}</code></Descriptions.Item>
              <Descriptions.Item label="租户名称">{detailRecord.tenantName}</Descriptions.Item>
              <Descriptions.Item label="联系人">{detailRecord.contactName || '-'}</Descriptions.Item>
              <Descriptions.Item label="联系电话">{detailRecord.contactPhone || '-'}</Descriptions.Item>
              <Descriptions.Item label="联系邮箱">{detailRecord.contactEmail || '-'}</Descriptions.Item>
              <Descriptions.Item label="到期时间">{detailRecord.expireTime ? dayjs(detailRecord.expireTime).format('YYYY-MM-DD HH:mm') : '永久'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{detailRecord.createTime?.replace('T', ' ')?.substring(0, 19)}</Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
    </>
  );
}
