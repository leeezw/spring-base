import React, { useState, useEffect } from 'react';
import {
  Table,
  Card,
  Button,
  Space,
  Typography,
  Modal,
  Form,
  Input,
  Select,
  Message,
  Popconfirm,
  Tag,
  InputNumber,
  DatePicker,
} from '@arco-design/web-react';
import { IconPlus, IconSearch } from '@arco-design/web-react/icon';
import axios from 'axios';
import dayjs from 'dayjs';
import styles from './style/index.module.less';

const { Title } = Typography;
const FormItem = Form.Item;
const Option = Select.Option;

interface Tenant {
  id: number;
  tenantCode: string;
  tenantName: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  expireTime?: string;
  accountCount: number;
  status: number;
  createTime: string;
}

function TenantManage() {
  const [data, setData] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showTotal: true,
    sizeCanChange: true,
  });
  const [searchTenantName, setSearchTenantName] = useState('');
  const [visible, setVisible] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
  const [form] = Form.useForm();

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '租户编码',
      dataIndex: 'tenantCode',
      width: 150,
    },
    {
      title: '租户名称',
      dataIndex: 'tenantName',
      width: 150,
    },
    {
      title: '联系人',
      dataIndex: 'contactName',
      width: 120,
    },
    {
      title: '联系电话',
      dataIndex: 'contactPhone',
      width: 150,
    },
    {
      title: '联系邮箱',
      dataIndex: 'contactEmail',
      width: 200,
    },
    {
      title: '账号数量',
      dataIndex: 'accountCount',
      width: 100,
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '正常' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right' as const,
      render: (_: any, record: Tenant) => (
        <Space>
          <Button type="text" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定要删除吗？"
            onOk={() => handleDelete(record.id)}
          >
            <Button type="text" status="danger" size="small">
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const params: any = {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      };
      if (searchTenantName) {
        params.tenantName = searchTenantName;
      }

      const res = await axios.get('/api/system/tenant/page', { params });
      const { code, data: resData, message } = res.data;

      if (code === 200) {
        setData(resData.records);
        setPagination({
          ...pagination,
          total: resData.total,
        });
      } else {
        Message.error(message || '获取租户列表失败');
      }
    } catch (error) {
      console.error('获取租户列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchData();
  };

  const handleAdd = () => {
    setEditingTenant(null);
    form.resetFields();
    setVisible(true);
  };

  const handleEdit = (tenant: Tenant) => {
    setEditingTenant(tenant);
    const formValues = {
      ...tenant,
      expireTime: tenant.expireTime ? dayjs(tenant.expireTime) : undefined,
    };
    form.setFieldsValue(formValues);
    setVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const res = await axios.delete(`/api/system/tenant/delete/${id}`);
      const { code, message } = res.data;

      if (code === 200) {
        Message.success('删除成功');
        fetchData();
      } else {
        Message.error(message || '删除失败');
      }
    } catch (error) {
      console.error('删除失败:', error);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validate();
      
      // 格式化日期
      const submitData = {
        ...values,
        expireTime: values.expireTime ? dayjs(values.expireTime).format('YYYY-MM-DD HH:mm:ss') : null,
      };

      let res;
      if (editingTenant) {
        res = await axios.put('/api/system/tenant/update', {
          ...submitData,
          id: editingTenant.id,
        });
      } else {
        res = await axios.post('/api/system/tenant/save', submitData);
      }

      const { code, message } = res.data;

      if (code === 200) {
        Message.success(editingTenant ? '更新成功' : '添加成功');
        setVisible(false);
        fetchData();
      } else {
        Message.error(message || '操作失败');
      }
    } catch (error) {
      console.error('提交失败:', error);
    }
  };

  const onChangeTable = ({ current, pageSize }) => {
    setPagination({
      ...pagination,
      current,
      pageSize,
    });
  };

  return (
    <Card>
      <Title heading={6}>租户管理</Title>
      
      <div className={styles['search-form']}>
        <Space>
          <Input
            style={{ width: 200 }}
            placeholder="搜索租户名称"
            prefix={<IconSearch />}
            value={searchTenantName}
            onChange={setSearchTenantName}
            onPressEnter={handleSearch}
          />
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={() => { setSearchTenantName(''); setPagination({ ...pagination, current: 1 }); fetchData(); }}>
            重置
          </Button>
        </Space>
      </div>

      <div className={styles['button-group']}>
        <Space>
          <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
            新增租户
          </Button>
        </Space>
      </div>

      <Table
        loading={loading}
        columns={columns}
        data={data}
        pagination={pagination}
        onChange={onChangeTable}
        scroll={{ x: 1800 }}
      />

      <Modal
        title={editingTenant ? '编辑租户' : '新增租户'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
      >
        <Form form={form} layout="vertical">
          <FormItem
            label="租户编码"
            field="tenantCode"
            rules={[{ required: true, message: '请输入租户编码' }]}
          >
            <Input placeholder="请输入租户编码" disabled={!!editingTenant} />
          </FormItem>

          <FormItem
            label="租户名称"
            field="tenantName"
            rules={[{ required: true, message: '请输入租户名称' }]}
          >
            <Input placeholder="请输入租户名称" />
          </FormItem>

          <FormItem label="联系人" field="contactName">
            <Input placeholder="请输入联系人" />
          </FormItem>

          <FormItem label="联系电话" field="contactPhone">
            <Input placeholder="请输入联系电话" />
          </FormItem>

          <FormItem label="联系邮箱" field="contactEmail">
            <Input placeholder="请输入联系邮箱" />
          </FormItem>

          <FormItem
            label="账号数量"
            field="accountCount"
            initialValue={10}
            rules={[{ required: true, message: '请输入账号数量' }]}
          >
            <InputNumber placeholder="请输入账号数量" min={1} style={{ width: '100%' }} />
          </FormItem>

          <FormItem label="过期时间" field="expireTime">
            <DatePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: '100%' }}
              placeholder="请选择过期时间"
            />
          </FormItem>

          <FormItem
            label="状态"
            field="status"
            initialValue={1}
            rules={[{ required: true }]}
          >
            <Select>
              <Option value={1}>正常</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </FormItem>
        </Form>
      </Modal>
    </Card>
  );
}

export default TenantManage;
