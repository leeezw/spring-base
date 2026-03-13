import { useState, useEffect, useCallback } from 'react';
import { Modal, Checkbox, List, Tag, Space, Empty, Skeleton, message } from 'antd';
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
 * 角色选择弹窗组件
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

  // 获取权限类型图标
  const getTypeIcon = (type) => {
    const iconMap = {
      'menu': <MenuOutlined />,
      'button': <AppstoreOutlined />,
      'api': <ApiOutlined />,
    };
    return iconMap[type] || <KeyOutlined />;
  };

  // 加载角色列表
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

  // 加载权限树
  const loadPermissionTree = useCallback(async () => {
    try {
      const res = await request.get('/system/permission/tree');
      if (res.code === 200 && res.data) {
        const tree = Array.isArray(res.data) ? res.data : [];
        setPermissionTree(tree);
      }
    } catch (error) {
      console.error('loadPermissionTree error:', error);
    }
  }, []);

  // 根据权限ID查找权限名称
  const findPermissionName = (permissionId, tree) => {
    const traverse = (nodes) => {
      for (const node of nodes) {
        if (node.id === permissionId) {
          return node.name;
        }
        if (node.children && node.children.length > 0) {
          const found = traverse(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    return traverse(tree);
  };

  // 渲染权限列表
  const renderPermissions = (permissionIds) => {
    if (!permissionIds || permissionIds.length === 0) {
      return <span style={{ color: '#999' }}>无权限</span>;
    }

    const permissionNames = permissionIds
      .map(id => findPermissionName(id, permissionTree))
      .filter(name => name);

    if (permissionNames.length === 0) {
      return <span style={{ color: '#999' }}>无权限</span>;
    }

    return (
      <div className="permission-tags">
        {permissionNames.slice(0, 5).map((name, index) => (
          <Tag key={index} size="small" style={{ marginBottom: '4px' }}>
            {name}
          </Tag>
        ))}
        {permissionNames.length > 5 && (
          <Tag size="small" style={{ marginBottom: '4px' }}>
            +{permissionNames.length - 5}
          </Tag>
        )}
      </div>
    );
  };

  // 初始化选中角色
  useEffect(() => {
    if (visible) {
      setSelectedRoleIds([...currentRoleIds]);
      loadRoles();
      loadPermissionTree();
    } else {
      setSelectedRoleIds([]);
    }
  }, [visible, currentRoleIds, loadRoles, loadPermissionTree]);

  // 处理角色选择变化
  const handleRoleChange = (roleId, checked) => {
    if (checked) {
      setSelectedRoleIds([...selectedRoleIds, roleId]);
    } else {
      setSelectedRoleIds(selectedRoleIds.filter(id => id !== roleId));
    }
  };

  // 处理全选
  const handleSelectAll = (checked) => {
    if (checked) {
      const enabledRoleIds = roles
        .filter(role => role.status === 1)
        .map(role => role.id);
      setSelectedRoleIds(enabledRoleIds);
    } else {
      setSelectedRoleIds([]);
    }
  };

  // 处理提交
  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      // 先获取用户完整信息，避免覆盖其他字段
      const userRes = await request.get(`/system/user/${userId}`);
      if (userRes.code !== 200 || !userRes.data) {
        throw new Error('获取用户信息失败');
      }

      const userData = userRes.data;
      
      // 调用更新用户接口，保留原有信息，只更新角色ID
      const res = await request.put('/system/user/update', {
        id: userId,
        nickname: userData.nickname,
        email: userData.email,
        phone: userData.phone,
        remark: userData.remark,
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

  const enabledRoles = roles.filter(role => role.status === 1);
  const allEnabledSelected = enabledRoles.length > 0 && 
    enabledRoles.every(role => selectedRoleIds.includes(role.id));

  return (
    <Modal
      title={`为用户 ${userName} 授予角色`}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确定"
      cancelText="取消"
      width={900}
      confirmLoading={submitting}
      className="role-select-modal"
      style={{ top: 40 }}
      styles={{ body: { maxHeight: 'calc(100vh - 200px)', overflow: 'hidden', display: 'flex', flexDirection: 'column' } }}
    >
      <div className="role-select-content">
        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : (
          <>
            {/* 全选控制 */}
            <div className="select-all-control">
              <Checkbox
                checked={allEnabledSelected}
                indeterminate={
                  selectedRoleIds.length > 0 && 
                  selectedRoleIds.length < enabledRoles.length
                }
                onChange={(e) => handleSelectAll(e.target.checked)}
              >
                全选已启用角色
              </Checkbox>
              <span className="selected-count">
                已选择 {selectedRoleIds.length} 个角色
              </span>
            </div>

            {/* 角色列表 */}
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
                      <List.Item
                        key={role.id}
                        className={`role-list-item ${isSelected ? 'selected' : ''} ${!isEnabled ? 'disabled' : ''}`}
                      >
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
                                  <span className="role-name">{role.name}</span>
                                  <span className="role-code">({role.code})</span>
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
    </Modal>
  );
}

