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
} from '@arco-design/web-react';
import { IconPlus, IconSearch } from '@arco-design/web-react/icon';
import axios from 'axios';
import styles from './style/index.module.less';

const { Title } = Typography;
const FormItem = Form.Item;
const Option = Select.Option;

interface Role {
  id: number;
  tenantId: number;
  roleCode: string;
  roleName: string;
  remark?: string;
  status: number;
  createTime: string;
}

function RoleManage() {
  const [data, setData] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showTotal: true,
    sizeCanChange: true,
  });
  const [searchRoleName, setSearchRoleName] = useState('');
  const [visible, setVisible] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [form] = Form.useForm();

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '角色编码',
      dataIndex: 'roleCode',
      width: 150,
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      width: 150,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 250,
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
      render: (_: any, record: Role) => (
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
      if (searchRoleName) {
        params.roleName = searchRoleName;
      }

      const res = await axios.get('/api/system/role/page', { params });
      const { code, data: resData, message } = res.data;

      if (code === 200) {
        setData(resData.records);
        setPagination({
          ...pagination,
          total: resData.total,
        });
      } else {
        Message.error(message || '获取角色列表失败');
      }
    } catch (error) {
      console.error('获取角色列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchData();
  };

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    setVisible(true);
  };

  const handleEdit = (role: Role) => {
    setEditingRole(role);
    form.setFieldsValue(role);
    setVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const res = await axios.delete(`/api/system/role/delete/${id}`);
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

      let res;
      if (editingRole) {
        res = await axios.put('/api/system/role/update', {
          ...values,
          id: editingRole.id,
        });
      } else {
        res = await axios.post('/api/system/role/save', values);
      }

      const { code, message } = res.data;

      if (code === 200) {
        Message.success(editingRole ? '更新成功' : '添加成功');
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
      <Title heading={6}>角色管理</Title>
      
      <div className={styles['search-form']}>
        <Space>
          <Input
            style={{ width: 200 }}
            placeholder="搜索角色名称"
            prefix={<IconSearch />}
            value={searchRoleName}
            onChange={setSearchRoleName}
            onPressEnter={handleSearch}
          />
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={() => { setSearchRoleName(''); setPagination({ ...pagination, current: 1 }); fetchData(); }}>
            重置
          </Button>
        </Space>
      </div>

      <div className={styles['button-group']}>
        <Space>
          <Button type="primary" icon={<IconPlus />} onClick={handleAdd}>
            新增角色
          </Button>
        </Space>
      </div>

      <Table
        loading={loading}
        columns={columns}
        data={data}
        pagination={pagination}
        onChange={onChangeTable}
        scroll={{ x: 1200 }}
      />

      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        autoFocus={false}
      >
        <Form form={form} layout="vertical">
          <FormItem
            label="角色编码"
            field="roleCode"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input placeholder="请输入角色编码" disabled={!!editingRole} />
          </FormItem>

          <FormItem
            label="角色名称"
            field="roleName"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </FormItem>

          <FormItem label="备注" field="remark">
            <Input.TextArea placeholder="请输入备注" rows={3} />
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

export default RoleManage;
