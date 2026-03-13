import { useState, useEffect, useCallback } from 'react';
import { Drawer, Checkbox, List, Tag, Space, Empty, Skeleton, message, Button } from 'antd';
import { 
  SafetyOutlined,
  ApiOutlined,
  MenuOutlined,
  AppstoreOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import './RoleSelectModal.css';

/**
 * 角色选择抽屉组件
 * 用于为用户授予角色
 */
export default function RoleSelectModal({ 
  visible, 
  userId, 
  userName,
  currentRoleIds = [],
  onCancel, 
  onOk 
}) {
  const [roles, setRoles] = useState([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [permissionTree, setPermissionTree] = useState([]);

  const getTypeIcon = (type) => {
    const iconMap = {
      'menu': <MenuOutlined />,
      'button': <AppstoreOutlined />,
      'api': <ApiOutlined />,
    };
    return iconMap[type] || <KeyOutlined />;
  };

  const loadRoles = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request.get('/system/role/page', { params: { pageNum: 1, pageSize: 100 } });
      if (res.code === 200 && res.data) {
        const roleList = res.data.records || (Array.isArray(res.data) ? res.data : []);
        setRoles(roleList);
      }
    } catch (error) {
      console.error('loadRoles error:', error);
      message.error('加载角色列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadPermissionTree = useCallback(async () => {
    try {
      const res = await request.get('/system/permission/tree');
      if (res.code === 200 && res.data) {
        setPermissionTree(Array.isArray(res.data) ? res.data : []);
      }
    } catch (error) {
      console.error('loadPermissionTree error:', error);
    }
  }, []);

  const findPermissionName = (permissionId, tree) => {
    const traverse = (nodes) => {
      for (const node of nodes) {
        if (node.id === permissionId) return node.permissionName || node.name;
        if (node.children?.length > 0) {
          const found = traverse(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    return traverse(tree);
  };

  const renderPermissions = (permissionIds) => {
    if (!permissionIds || permissionIds.length === 0) {
      return <span style={{ color: '#999' }}>无权限</span>;
    }
    const names = permissionIds.map(id => findPermissionName(id, permissionTree)).filter(Boolean);
    if (names.length === 0) return <span style={{ color: '#999' }}>无权限</span>;
    return (
      <div className="permission-tags">
        {names.slice(0, 5).map((name, i) => <Tag key={i} size="small" style={{ marginBottom: 4 }}>{name}</Tag>)}
        {names.length > 5 && <Tag size="small" style={{ marginBottom: 4 }}>+{names.length - 5}</Tag>}
      </div>
    );
  };

  useEffect(() => {
    if (visible) {
      setSelectedRoleIds([...currentRoleIds]);
      loadRoles();
      loadPermissionTree();
    } else {
      setSelectedRoleIds([]);
    }
  }, [visible, currentRoleIds, loadRoles, loadPermissionTree]);

  const handleRoleChange = (roleId, checked) => {
    setSelectedRoleIds(checked ? [...selectedRoleIds, roleId] : selectedRoleIds.filter(id => id !== roleId));
  };

  const handleSelectAll = (checked) => {
    if (checked) {
      setSelectedRoleIds(roles.filter(r => r.status === 1).map(r => r.id));
    } else {
      setSelectedRoleIds([]);
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const userRes = await request.get(`/system/user/${userId}`);
      if (userRes.code !== 200 || !userRes.data) throw new Error('获取用户信息失败');
      const userData = userRes.data;
      const res = await request.put('/system/user/update', {
        id: userId,
        nickname: userData.nickname,
        email: userData.email,
        phone: userData.phone,
        roleIds: selectedRoleIds,
      });
      if (res.code === 200) {
        message.success('角色授予成功');
        onOk?.(selectedRoleIds);
      } else {
        message.error(res.message || '角色授予失败');
      }
    } catch (error) {
      message.error(error.message || '角色授予失败');
    } finally {
      setSubmitting(false);
    }
  };

  const enabledRoles = roles.filter(r => r.status === 1);
  const allEnabledSelected = enabledRoles.length > 0 && enabledRoles.every(r => selectedRoleIds.includes(r.id));

  return (
    <Drawer
      title={`为用户 ${userName} 授予角色`}
      open={visible}
      onClose={onCancel}
      width={520}
      className="role-select-drawer"
      extra={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" loading={submitting} onClick={handleSubmit}>确定</Button>
        </Space>
      }
    >
      <div className="role-select-content">
        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : (
          <>
            <div className="select-all-control">
              <Checkbox
                checked={allEnabledSelected}
                indeterminate={selectedRoleIds.length > 0 && selectedRoleIds.length < enabledRoles.length}
                onChange={(e) => handleSelectAll(e.target.checked)}
              >
                全选已启用角色
              </Checkbox>
              <span className="selected-count">已选择 {selectedRoleIds.length} 个角色</span>
            </div>
            <div className="role-list-container">
              {roles.length === 0 ? (
                <Empty description="暂无角色数据" />
              ) : (
                <List
                  dataSource={roles}
                  renderItem={(role) => {
                    const isSelected = selectedRoleIds.includes(role.id);
                    const isEnabled = role.status === 1;
                    return (
                      <List.Item key={role.id} className={`role-list-item ${isSelected ? 'selected' : ''} ${!isEnabled ? 'disabled' : ''}`}>
                        <div className="role-item-content">
                          <Checkbox
                            checked={isSelected}
                            disabled={!isEnabled}
                            onChange={(e) => handleRoleChange(role.id, e.target.checked)}
                            className="role-checkbox"
                          >
                            <div className="role-info">
                              <div className="role-header">
                                <Space>
                                  <SafetyOutlined style={{ color: isEnabled ? '#3f8cff' : '#999' }} />
                                  <span className="role-name">{role.roleName || role.name}</span>
                                  <span className="role-code">({role.roleCode || role.code})</span>
                                  <Tag color={isEnabled ? 'success' : 'error'} size="small">
                                    {isEnabled ? '启用' : '禁用'}
                                  </Tag>
                                </Space>
                              </div>
                              <div className="role-permissions">
                                <span className="permission-label">权限：</span>
                                {renderPermissions(role.permissionIds)}
                              </div>
                            </div>
                          </Checkbox>
                        </div>
                      </List.Item>
                    );
                  }}
                />
              )}
            </div>
          </>
        )}
      </div>
    </Drawer>
  );
}
