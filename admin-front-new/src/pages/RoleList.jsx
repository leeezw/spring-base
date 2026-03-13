import { useState, useRef, useCallback, useEffect } from 'react';
import { Card, Statistic, Button, Space, Tag, Modal, Form, message, Drawer, Tree, Spin } from 'antd';
import { 
  SafetyOutlined, 
  CheckCircleOutlined, 
  StopOutlined, 
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PoweroffOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import RoleForm from '../components/RoleForm.jsx';
import TableSearchForm from '../components/TableSearchForm.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';
import { usePageToolbar } from '../components/AppLayout.jsx';
import './RoleList.css';

export default function RoleList() {
  const actionRef = useRef();
  const [stats, setStats] = useState({
    total: 0,
    enabled: 0,
    disabled: 0,
  });
  const [form] = Form.useForm();
  const [filterForm] = Form.useForm();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [filterParams, setFilterParams] = useState({ status: 'all' });
  const [permissionTree, setPermissionTree] = useState([]);
  const [permissionTreeLoading, setPermissionTreeLoading] = useState(true);
  const [grantDrawerVisible, setGrantDrawerVisible] = useState(false);
  const [grantLoading, setGrantLoading] = useState(false);
  const [grantTree, setGrantTree] = useState([]);
  const [grantCheckedKeys, setGrantCheckedKeys] = useState([]);
  const [grantRole, setGrantRole] = useState(null);
  const [grantType, setGrantType] = useState('permission'); // 'permission' | 'menu'
  const { statsVisible, setStatsVisible, setShowStatsToggle } = usePageToolbar();
  const debounceTimerRef = useRef(null);
  
  // 组件挂载时显示统计切换按钮
  useEffect(() => {
    setShowStatsToggle(true);
    return () => {
      setShowStatsToggle(false);
    };
  }, [setShowStatsToggle]);

  // 防抖处理筛选
  const handleFilterChange = useCallback(() => {
    // 清除之前的定时器
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    // 设置新的定时器，500ms 后执行搜索
    debounceTimerRef.current = setTimeout(() => {
      filterForm.submit();
    }, 500);
  }, [filterForm]);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // 加载权限树
  useEffect(() => {
    const loadPermissionTree = async () => {
      setPermissionTreeLoading(true);
      try {
        const res = await request.get('/system/permission/tree');
        if (res.code === 200 && res.data) {
          const tree = Array.isArray(res.data) ? res.data : [];
          setPermissionTree(tree);
        }
      } catch (error) {
        console.error('loadPermissionTree error:', error);
      } finally {
        setPermissionTreeLoading(false);
      }
    };
    loadPermissionTree();
  }, []);

  // 请求函数 - 适配 ProTableV2
  const fetchRoles = async (params) => {
    try {
    const res = await request.get('/system/role/page', { params: { pageNum: params.current || 1, pageSize: params.pageSize || 10, roleName: params.keyword || undefined } });
      if (res.code === 200 && res.data) {
        const pageData = res.data;
        const list = pageData.records || [];
        
        // 计算统计数据
        const enabled = list.filter(item => item.status === 1).length;
        const disabled = list.filter(item => item.status === 0).length;
        
        setStats({
          total: pageData.total || list.length,
          enabled: enabled,
          disabled: disabled,
        });
        
        // 返回 ProTableV2 期望的格式
        return {
          code: 200,
          data: {
            list: list,
            total: pageData.total || list.length
          }
        };
      }
    return res;
    } catch (error) {
      console.error('fetchRoles error:', error);
      return {
        code: 500,
        data: { list: [], total: 0 }
      };
    }
  };

  // 处理数据变化（仅用于通知，不触发刷新，避免无限循环）
  const handleDataChange = (data, total) => {
    console.log('handleDataChange', data, total);
  };

  const handleRefresh = () => {
    actionRef.current?.reload();
  };

  const handleAddRole = () => {
    setEditingRole(null);
    form.resetFields();
    // 确保表单字段完全清空
    form.setFieldsValue({
      code: undefined,
      name: undefined,
      status: 1,
      permissionIds: [],
    });
    setModalVisible(true);
  };

  const handleEditRole = (record) => {
    setEditingRole(record);
    form.setFieldsValue({
      code: record.roleCode,
      name: record.roleName,
      permissionIds: record.permissionIds || [],
      // 编辑时不设置状态，状态通过独立的状态按钮修改
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values) => {
    setSubmitLoading(true);
    try {
      // 确保 permissionIds 是数组格式
      const submitData = {
        roleCode: values.code,
        roleName: values.name,
        description: values.description || '',
        status: values.status ?? 1,
        permissionIds: Array.isArray(values.permissionIds) ? values.permissionIds : [],
      };
      
      if (editingRole) {
        // 编辑角色
        const res = await request.put('/system/role', { ...submitData, id: editingRole.id });
        if (res.code === 200) {
          message.success('角色更新成功');
          setModalVisible(false);
          form.resetFields();
          setEditingRole(null);
          handleRefresh();
        } else {
          message.error(res.message || '更新失败');
        }
      } else {
        // 新增角色
        const res = await request.post('/system/role', submitData);
        if (res.code === 200) {
          message.success('角色创建成功');
          setModalVisible(false);
          form.resetFields();
          // 确保表单字段完全清空
          form.setFieldsValue({
            code: undefined,
            name: undefined,
            status: 1,
            permissionIds: [],
          });
          setEditingRole(null);
          handleRefresh();
        } else {
          message.error(res.message || '创建失败');
        }
      }
    } catch (error) {
      message.error(error.message || '操作失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleCancel = () => {
    setModalVisible(false);
    form.resetFields();
    // 清空表单字段
    form.setFieldsValue({
      code: undefined,
      name: undefined,
      status: 1,
      permissionIds: [],
    });
    setEditingRole(null);
  };

  const buildTreeData = useCallback((nodes = []) => {
    return nodes.map(node => ({
      title: (
        <span>
          <strong>{node.name}</strong>
          <span style={{ marginLeft: 6, color: '#999' }}>({node.code})</span>
        </span>
      ),
      key: node.id?.toString(),
      children: node.children && node.children.length > 0 ? buildTreeData(node.children) : undefined,
    }));
  }, []);

  // 打开权限/菜单分配抽屉
  const handleOpenGrantDrawer = async (record, type = 'permission') => {
    setGrantDrawerVisible(true);
    setGrantRole(record);
    setGrantType(type);
    setGrantLoading(true);
    try {
      // 加载树数据 + 已选中的ID
      let treeRes, checkedRes;
      if (type === 'permission') {
        [treeRes, checkedRes] = await Promise.all([
          request.get('/system/permission/tree'),
          request.get(`/system/relation/role/${record.id}/permissions`),
        ]);
      } else {
        [treeRes, checkedRes] = await Promise.all([
          request.get('/system/menu/tree'),
          request.get(`/system/relation/role/${record.id}/menus`),
        ]);
      }
      
      if (treeRes.code === 200 && treeRes.data) {
        const transformTree = (nodes) => nodes.map(node => ({
          title: type === 'permission' 
            ? `${node.permissionName || node.name} (${node.permissionCode || node.code})`
            : node.menuName,
          key: node.id.toString(),
          children: node.children?.length > 0 ? transformTree(node.children) : undefined,
        }));
        setGrantTree(transformTree(treeRes.data));
      }
      
      if (checkedRes.code === 200 && checkedRes.data) {
        setGrantCheckedKeys(checkedRes.data.map(id => id.toString()));
      } else {
        setGrantCheckedKeys([]);
      }
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setGrantLoading(false);
    }
  };

  const handleGrantCheck = (checkedKeysValue) => {
    if (Array.isArray(checkedKeysValue)) {
      setGrantCheckedKeys(checkedKeysValue);
    } else if (checkedKeysValue?.checked) {
      setGrantCheckedKeys(checkedKeysValue.checked);
    }
  };

  const handleGrantSubmit = async () => {
    if (!grantRole) return;
    setGrantLoading(true);
    try {
      const ids = grantCheckedKeys.map(key => Number(key));
      const url = grantType === 'permission'
        ? `/system/relation/role/${grantRole.id}/permissions`
        : `/system/relation/role/${grantRole.id}/menus`;
      
      const res = await request.post(url, { ids });
      if (res.code === 200) {
        message.success(grantType === 'permission' ? '权限分配成功' : '菜单分配成功');
        setGrantDrawerVisible(false);
        setGrantRole(null);
        handleRefresh();
      } else {
        message.error(res.message || '分配失败');
      }
    } catch (error) {
      message.error(error.message || '分配失败');
    } finally {
      setGrantLoading(false);
    }
  };

  // 修改角色状态
  const handleChangeStatus = async (record) => {
    const newStatus = record.status === 1 ? 0 : 1;
    const statusText = newStatus === 1 ? '启用' : '禁用';
    
    Modal.confirm({
      title: `确认${statusText}角色`,
      content: `确定要${statusText}角色 "${record.roleName || record.roleCode}" 吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 更新角色状态
          const res = await request.put('/system/role', {
            id: record.id,
            roleCode: record.roleCode,
            roleName: record.roleName,
            status: newStatus
          });
          
          if (res.code === 200) {
            message.success(`角色已${statusText}`);
            handleRefresh();
          } else {
            message.error(res.message || `${statusText}失败`);
          }
        } catch (error) {
          message.error(error.message || `${statusText}失败`);
        }
      }
    });
  };

  // 删除角色
  const handleDeleteRole = async (record) => {
    Modal.confirm({
      title: '确认删除角色',
      content: `确定要删除角色 "${record.roleName || record.roleCode}" 吗？此操作不可恢复。`,
      okText: '确认',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          const res = await request.delete(`/system/role/${record.id}`);
          if (res.code === 200) {
            message.success('角色删除成功');
            handleRefresh();
          } else {
            message.error(res.message || '删除失败');
          }
        } catch (error) {
          message.error(error.message || '删除失败');
        }
      }
    });
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      hideInSearch: true,
    },
    {
      title: '角色编码',
      dataIndex: 'roleCode',
      key: 'code',
      ellipsis: true,
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'name',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      valueType: 'select',
      valueEnum: {
        1: {
          text: '启用',
          status: 'Success',
        },
        0: {
          text: '禁用',
          status: 'Error',
        },
      },
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '权限数量',
      dataIndex: 'permissionIds',
      key: 'permissionIds',
      hideInSearch: true,
      render: (permissionIds) => permissionIds?.length || 0,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      sorter: true,
      width: 180,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      valueType: 'dateTime',
      hideInSearch: true,
      sorter: true,
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      valueType: 'option',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button 
            type="text" 
            icon={<SafetyOutlined />} 
            size="small"
            title="分配权限"
            onClick={() => handleOpenGrantDrawer(record, 'permission')}
          />
          <Button 
            type="text" 
            icon={<MenuOutlined />} 
            size="small"
            title="分配菜单"
            onClick={() => handleOpenGrantDrawer(record, 'menu')}
          />
          <Button 
            type="text" 
            icon={<EditOutlined />} 
            size="small"
            title="编辑"
            onClick={() => handleEditRole(record)}
          />
          <Button 
            type="text" 
            icon={<PoweroffOutlined />} 
            size="small"
            title={record.status === 1 ? '禁用' : '启用'}
            danger={record.status === 1}
            onClick={() => handleChangeStatus(record)}
          />
          <Button 
            type="text" 
            icon={<DeleteOutlined />} 
            size="small"
            danger
            title="删除"
            onClick={() => handleDeleteRole(record)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div className="role-list-page">
      {/* 统计卡片 */}
      {statsVisible && (
        <div className="stats-grid">
          <Card className="stat-card">
            <Statistic
              title="角色总数"
              value={stats.total}
              prefix={<SafetyOutlined style={{ color: '#3f8cff' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
          <Card className="stat-card">
            <Statistic
              title="已启用"
              value={stats.enabled}
              prefix={<CheckCircleOutlined style={{ color: '#22c55e' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
          <Card className="stat-card">
            <Statistic
              title="已禁用"
              value={stats.disabled}
              prefix={<StopOutlined style={{ color: '#fb923c' }} />}
              styles={{ content: { color: '#0a1629' } }}
            />
          </Card>
        </div>
      )}

      {/* 搜索表单区域 */}
      <div className="search-section">
        <TableSearchForm
          form={filterForm}
          initialValues={{ status: 'all' }}
          onFinish={async (values) => {
            // 当筛选条件变化时，更新筛选参数并触发表格刷新
            const newFilterParams = { ...values };
            // 处理 status 参数（如果存在且是 'all'，则移除）
            if (newFilterParams.status === 'all') {
              delete newFilterParams.status;
            }
            // 处理日期时间范围参数
            if (values.dateRange) {
              newFilterParams.startTime = values.startTime;
              newFilterParams.endTime = values.endTime;
              // 移除 dateRange 对象，避免发送到后端
              delete newFilterParams.dateRange;
            }
            setFilterParams(newFilterParams);
            // 触发表格刷新，ProTable 会将 filterParams 合并到 requestParams 中
            actionRef.current?.reload();
          }}
          onKeywordChange={handleFilterChange}
          config={{
            showKeyword: true,
            keywordPlaceholder: '搜索角色编码或名称',
            showStatus: true,
            statusOptions: [
              { label: '全部', value: 'all' },
              { label: '启用', value: 1 },
              { label: '禁用', value: 0 },
            ],
            showDateRange: true,
            dateRangePlaceholder: ['创建开始时间', '创建结束时间'],
            dateRangeStartName: 'startTime',
            dateRangeEndName: 'endTime',
          }}
        />
      </div>

      {/* 数据表格 */}
      <ProTableV2
        actionRef={actionRef}
        headerTitle="角色列表"
        columns={columns}
        request={fetchRoles}
        rowKey="id"
        onDataChange={handleDataChange}
        search={false}
        pagination={false}
        toolbar={{
          actions: [
            <Button 
              key="add" 
              type="primary" 
              icon={<PlusOutlined />} 
              onClick={handleAddRole}
            >
              新增角色
            </Button>,
          ],
        }}
        params={filterParams}
      />

      {/* 新增/编辑角色抽屉 */}
      <Drawer
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onClose={handleCancel}
        width={520}
        destroyOnClose
        extra={
          <Space>
            <Button onClick={handleCancel}>取消</Button>
            <Button type="primary" onClick={() => form.submit()} loading={submitLoading}>
              {editingRole ? '更新' : '创建'}
            </Button>
          </Space>
        }
      >
        <RoleForm
          key={editingRole ? `edit-${editingRole.id}` : 'add'}
          form={form}
          initialValues={editingRole}
          onFinish={handleSubmit}
          permissionTree={permissionTree}
          permissionTreeLoading={permissionTreeLoading}
        />
      </Drawer>

      <Drawer
        title={grantRole ? `${grantType === 'permission' ? '分配权限' : '分配菜单'} - ${grantRole.roleName || grantRole.roleCode}` : '分配'}
        placement="right"
        width={520}
        open={grantDrawerVisible}
        onClose={() => {
          setGrantDrawerVisible(false);
          setGrantRole(null);
        }}
        extra={(
          <Space>
            <Button onClick={() => setGrantDrawerVisible(false)}>取消</Button>
            <Button type="primary" loading={grantLoading} onClick={handleGrantSubmit}>
              保存
            </Button>
          </Space>
        )}
      >
        {grantLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin />
          </div>
        ) : (
          <Tree
            checkable
            selectable={false}
            treeData={grantTree}
            checkedKeys={grantCheckedKeys}
            onCheck={handleGrantCheck}
            defaultExpandAll
          />
        )}
      </Drawer>
    </div>
  );
}
