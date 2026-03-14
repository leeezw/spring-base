import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Toast,
  Tag,
  Space,
  Popconfirm,
  Typography,
} from '@douyinfe/semi-ui';
import { IconSearch, IconPlus, IconEdit, IconDelete, IconRefresh } from '@douyinfe/semi-icons';
import axios from 'axios';
import styles from './style/index.module.less';

const { Title, Text } = Typography;

export default function UserManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formApi, setFormApi] = useState(null);
  const [searchFormApi, setSearchFormApi] = useState(null);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 70,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      width: 140,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      width: 140,
      render: (text) => text || <Text type="quaternary">-</Text>,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      width: 220,
      render: (text) => text || <Text type="quaternary">-</Text>,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
      render: (text) => text || <Text type="quaternary">-</Text>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'red'} type="light">
          {status === 1 ? '正常' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 170,
      render: (text) => text ? text.replace('T', ' ').substring(0, 19) : '-',
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right' as const,
      render: (text, record) => (
        <Space spacing={4}>
          <Button
            theme="borderless"
            type="primary"
            icon={<IconEdit size="small" />}
            size="small"
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            content="删除后不可恢复，是否继续？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button
              theme="borderless"
              type="danger"
              icon={<IconDelete size="small" />}
              size="small"
            >
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
      const values = searchFormApi?.getValues() || {};
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
      Toast.error('获取用户列表失败');
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
    searchFormApi?.reset();
    fetchData(1, pagination.pageSize);
  };

  const handleAdd = () => {
    setEditingId(null);
    setTimeout(() => {
      formApi?.reset();
      formApi?.setValues({ status: 1 });
    }, 0);
    setVisible(true);
  };

  const handleEdit = async (record) => {
    setEditingId(record.id);
    setVisible(true);
    try {
      const res = await axios.get(`/api/system/user/${record.id}`);
      if (res.data.code === 200) {
        setTimeout(() => {
          formApi?.setValues(res.data.data);
        }, 0);
      }
    } catch (error) {
      Toast.error('获取用户信息失败');
    }
  };

  const handleDelete = async (id) => {
    try {
      const res = await axios.delete(`/api/system/user/delete/${id}`);
      if (res.data.code === 200) {
        Toast.success('删除成功');
        fetchData();
      } else {
        Toast.error(res.data.message || '删除失败');
      }
    } catch (error) {
      Toast.error('删除失败');
    }
  };

  const handleSubmit = () => {
    formApi?.validate()
      .then(async (values) => {
        const url = editingId
          ? '/api/system/user/update'
          : '/api/system/user/save';
        const method = editingId ? 'put' : 'post';
        const submitData = editingId ? { ...values, id: editingId } : values;

        const res = await axios[method](url, submitData);
        if (res.data.code === 200) {
          Toast.success(editingId ? '更新成功' : '新增成功');
          setVisible(false);
          fetchData();
        } else {
          Toast.error(res.data.message || '操作失败');
        }
      })
      .catch((errors) => {
        console.error('表单验证失败:', errors);
      });
  };

  const handlePageChange = (currentPage) => {
    fetchData(currentPage, pagination.pageSize);
  };

  return (
    <div className={styles.container}>
      {/* 搜索区域 */}
      <div className={styles.searchBar}>
        <Form
          layout="horizontal"
          getFormApi={(api) => setSearchFormApi(api)}
          labelPosition="inset"
        >
          <div className={styles.searchRow}>
            <div className={styles.searchFields}>
              <Form.Input
                field="username"
                label="用户名"
                placeholder="搜索用户名"
                style={{ width: 240 }}
                prefix={<IconSearch />}
                showClear
                onEnterPress={handleSearch}
              />
            </div>
            <Space>
              <Button type="primary" icon={<IconSearch />} onClick={handleSearch}>
                查询
              </Button>
              <Button icon={<IconRefresh />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </div>
        </Form>
      </div>

      {/* 表格区域 */}
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <Title heading={6} style={{ margin: 0 }}>用户列表</Title>
          <Space>
            <Text type="quaternary">共 {pagination.total} 条</Text>
            <Button
              type="primary"
              icon={<IconPlus />}
              onClick={handleAdd}
            >
              新增用户
            </Button>
          </Space>
        </div>
        <Table
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{
            currentPage: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            onPageChange: handlePageChange,
            showSizeChanger: true,
            pageSizeOpts: [10, 20, 50],
            showTotal: true,
            style: { padding: '16px 20px' },
          }}
          rowKey="id"
          size="middle"
          bordered={false}
          empty={
            <div style={{ padding: '40px 0', color: 'var(--semi-color-text-2)' }}>
              暂无数据
            </div>
          }
        />
      </div>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingId ? '编辑用户' : '新增用户'}
        visible={visible}
        onOk={handleSubmit}
        onCancel={() => setVisible(false)}
        width={520}
        okText="确定"
        cancelText="取消"
        maskClosable={false}
      >
        <Form
          labelPosition="left"
          labelWidth={80}
          labelAlign="right"
          getFormApi={(api) => setFormApi(api)}
          style={{ padding: '8px 0' }}
        >
          <Form.Input
            field="username"
            label="用户名"
            placeholder="请输入用户名"
            disabled={!!editingId}
            rules={[{ required: true, message: '请输入用户名' }]}
          />
          <Form.Input
            field="nickname"
            label="昵称"
            placeholder="请输入昵称"
          />
          {!editingId && (
            <Form.Input
              field="password"
              label="密码"
              mode="password"
              placeholder="请输入密码"
              rules={[{ required: true, message: '请输入密码' }]}
            />
          )}
          <Form.Input
            field="email"
            label="邮箱"
            placeholder="请输入邮箱"
            rules={[{ type: 'email', message: '邮箱格式不正确' }]}
          />
          <Form.Input
            field="phone"
            label="手机号"
            placeholder="请输入手机号"
          />
          <Form.Select
            field="status"
            label="状态"
            initValue={1}
            style={{ width: '100%' }}
          >
            <Select.Option value={1}>正常</Select.Option>
            <Select.Option value={0}>禁用</Select.Option>
          </Form.Select>
        </Form>
      </Modal>
    </div>
  );
}
