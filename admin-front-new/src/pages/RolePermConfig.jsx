import { useState, useCallback, useEffect, useMemo } from 'react';
import { Card, Tree, Button, message, Spin, Empty, Tag, Tabs, Badge, Space, Tooltip, Typography } from 'antd';
import {
  SafetyOutlined,
  MenuOutlined,
  CheckCircleOutlined,
  SaveOutlined,
  UndoOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import './RolePermConfig.css';

const { Text, Title } = Typography;

export default function RolePermConfig() {
  // 角色列表
  const [roles, setRoles] = useState([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [selectedRole, setSelectedRole] = useState(null);

  // 权限树
  const [permTree, setPermTree] = useState([]);
  const [permTreeLoading, setPermTreeLoading] = useState(false);
  const [checkedPermKeys, setCheckedPermKeys] = useState([]);
  const [savedPermKeys, setSavedPermKeys] = useState([]);

  // 菜单树
  const [menuTree, setMenuTree] = useState([]);
  const [menuTreeLoading, setMenuTreeLoading] = useState(false);
  const [checkedMenuKeys, setCheckedMenuKeys] = useState([]);
  const [savedMenuKeys, setSavedMenuKeys] = useState([]);

  // 保存状态
  const [permSaving, setPermSaving] = useState(false);
  const [menuSaving, setMenuSaving] = useState(false);

  const [activeTab, setActiveTab] = useState('permission');

  // 加载角色列表
  const loadRoles = useCallback(async () => {
    setRolesLoading(true);
    try {
      const res = await request.get('/system/role/page', { params: { page: 1, size: 100 } });
      if (res.code === 200) {
        const list = res.data?.records || res.data?.list || [];
        setRoles(list);
      }
    } catch (e) {
      message.error('加载角色失败');
    } finally {
      setRolesLoading(false);
    }
  }, []);

  // 加载权限树
  const loadPermTree = useCallback(async () => {
    setPermTreeLoading(true);
    try {
      const res = await request.get('/system/permission/tree');
      if (res.code === 200) {
        setPermTree(Array.isArray(res.data) ? res.data : []);
      }
    } catch (e) {
      message.error('加载权限树失败');
    } finally {
      setPermTreeLoading(false);
    }
  }, []);

  // 加载菜单树
  const loadMenuTree = useCallback(async () => {
    setMenuTreeLoading(true);
    try {
      const res = await request.get('/system/menu/tree');
      if (res.code === 200) {
        setMenuTree(Array.isArray(res.data) ? res.data : []);
      }
    } catch (e) {
      message.error('加载菜单树失败');
    } finally {
      setMenuTreeLoading(false);
    }
  }, []);

  // 加载角色已有权限
  const loadRolePermissions = useCallback(async (roleId) => {
    try {
      const res = await request.get(`/system/relation/role/${roleId}/permissions`);
      if (res.code === 200) {
        const ids = (res.data || []).map(id => id.toString());
        setCheckedPermKeys(ids);
        setSavedPermKeys(ids);
      }
    } catch (e) {
      console.error('加载角色权限失败', e);
    }
  }, []);

  // 加载角色已有菜单
  const loadRoleMenus = useCallback(async (roleId) => {
    try {
      const res = await request.get(`/system/relation/role/${roleId}/menus`);
      if (res.code === 200) {
        const ids = (res.data || []).map(id => id.toString());
        setCheckedMenuKeys(ids);
        setSavedMenuKeys(ids);
      }
    } catch (e) {
      console.error('加载角色菜单失败', e);
    }
  }, []);

  useEffect(() => {
    loadRoles();
    loadPermTree();
    loadMenuTree();
  }, [loadRoles, loadPermTree, loadMenuTree]);

  // 选中角色
  const handleSelectRole = useCallback((role) => {
    setSelectedRole(role);
    loadRolePermissions(role.id);
    loadRoleMenus(role.id);
  }, [loadRolePermissions, loadRoleMenus]);

  // 权限树格式转换
  const buildTreeData = useCallback((nodes, type) => {
    if (!nodes || !nodes.length) return [];
    return nodes.map(node => {
      const isPerm = type === 'permission';
      const name = isPerm ? node.permissionName : node.menuName;
      const code = isPerm ? node.permissionCode : node.path;
      const nodeType = isPerm ? node.permissionType : null;
      
      const typeLabel = nodeType === 1 ? '菜单' : nodeType === 2 ? '按钮' : nodeType === 3 ? '接口' : '';
      const typeColor = nodeType === 1 ? 'blue' : nodeType === 2 ? 'green' : nodeType === 3 ? 'orange' : 'default';
      
      return {
        key: node.id.toString(),
        title: (
          <span className="tree-node-title">
            <span className="tree-node-name">{name}</span>
            <span className="tree-node-code">{code}</span>
            {typeLabel && <Tag color={typeColor} className="tree-node-tag">{typeLabel}</Tag>}
          </span>
        ),
        children: node.children?.length > 0 ? buildTreeData(node.children, type) : undefined,
      };
    });
  }, []);

  const permTreeData = useMemo(() => buildTreeData(permTree, 'permission'), [permTree, buildTreeData]);
  const menuTreeData = useMemo(() => buildTreeData(menuTree, 'menu'), [menuTree, buildTreeData]);

  // 获取所有叶子节点key
  const getAllLeafKeys = useCallback((nodes) => {
    const keys = [];
    const traverse = (items) => {
      items.forEach(item => {
        if (!item.children || item.children.length === 0) {
          keys.push(item.key);
        } else {
          traverse(item.children);
        }
      });
    };
    traverse(nodes);
    return keys;
  }, []);

  // 保存权限
  const handleSavePerm = useCallback(async () => {
    if (!selectedRole) return;
    setPermSaving(true);
    try {
      const ids = checkedPermKeys.map(k => parseInt(k));
      const res = await request.post(`/system/relation/role/${selectedRole.id}/permissions`, { ids });
      if (res.code === 200) {
        message.success('权限保存成功');
        setSavedPermKeys([...checkedPermKeys]);
      } else {
        message.error(res.message || '保存失败');
      }
    } catch (e) {
      message.error('保存失败');
    } finally {
      setPermSaving(false);
    }
  }, [selectedRole, checkedPermKeys]);

  // 保存菜单
  const handleSaveMenu = useCallback(async () => {
    if (!selectedRole) return;
    setMenuSaving(true);
    try {
      const ids = checkedMenuKeys.map(k => parseInt(k));
      const res = await request.post(`/system/relation/role/${selectedRole.id}/menus`, { ids });
      if (res.code === 200) {
        message.success('菜单保存成功');
        setSavedMenuKeys([...checkedMenuKeys]);
      } else {
        message.error(res.message || '保存失败');
      }
    } catch (e) {
      message.error('保存失败');
    } finally {
      setMenuSaving(false);
    }
  }, [selectedRole, checkedMenuKeys]);

  // 是否有未保存的变更
  const permChanged = useMemo(() => {
    const a = [...checkedPermKeys].sort().join(',');
    const b = [...savedPermKeys].sort().join(',');
    return a !== b;
  }, [checkedPermKeys, savedPermKeys]);

  const menuChanged = useMemo(() => {
    const a = [...checkedMenuKeys].sort().join(',');
    const b = [...savedMenuKeys].sort().join(',');
    return a !== b;
  }, [checkedMenuKeys, savedMenuKeys]);

  // 全选/全不选
  const handleSelectAllPerm = useCallback(() => {
    const allKeys = getAllLeafKeys(permTreeData);
    // 还需要包含非叶子节点
    const allNodeKeys = [];
    const traverse = (items) => {
      items.forEach(item => {
        allNodeKeys.push(item.key);
        if (item.children) traverse(item.children);
      });
    };
    traverse(permTreeData);
    setCheckedPermKeys(allNodeKeys);
  }, [permTreeData, getAllLeafKeys]);

  const handleClearAllPerm = useCallback(() => {
    setCheckedPermKeys([]);
  }, []);

  const handleSelectAllMenu = useCallback(() => {
    const allNodeKeys = [];
    const traverse = (items) => {
      items.forEach(item => {
        allNodeKeys.push(item.key);
        if (item.children) traverse(item.children);
      });
    };
    traverse(menuTreeData);
    setCheckedMenuKeys(allNodeKeys);
  }, [menuTreeData]);

  const handleClearAllMenu = useCallback(() => {
    setCheckedMenuKeys([]);
  }, []);

  // 统计
  const permCount = checkedPermKeys.length;
  const menuCount = checkedMenuKeys.length;

  // 统计所有权限节点数
  const totalPermCount = useMemo(() => {
    let count = 0;
    const traverse = (items) => { items?.forEach(item => { count++; if (item.children) traverse(item.children); }); };
    traverse(permTree);
    return count;
  }, [permTree]);

  const totalMenuCount = useMemo(() => {
    let count = 0;
    const traverse = (items) => { items?.forEach(item => { count++; if (item.children) traverse(item.children); }); };
    traverse(menuTree);
    return count;
  }, [menuTree]);

  return (
    <div className="role-perm-config">
      {/* 左侧角色列表 */}
      <div className="role-panel">
        <div className="role-panel-header">
          <TeamOutlined /> 角色列表
          <Text type="secondary" className="role-panel-hint">选择角色配置权限</Text>
        </div>
        <Spin spinning={rolesLoading}>
          <div className="role-card-list">
            {roles.length === 0 && !rolesLoading && (
              <Empty description="暂无角色" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
            {roles.map(role => (
              <div
                key={role.id}
                className={`role-card ${selectedRole?.id === role.id ? 'role-card-active' : ''}`}
                onClick={() => handleSelectRole(role)}
              >
                <div className="role-card-main">
                  <div className="role-card-name">{role.roleName}</div>
                  <div className="role-card-code">{role.roleCode}</div>
                </div>
                <div className="role-card-meta">
                  <Tag color={role.status === 1 ? 'success' : 'default'}>
                    {role.status === 1 ? '启用' : '禁用'}
                  </Tag>
                </div>
                {selectedRole?.id === role.id && (
                  <CheckCircleOutlined className="role-card-check" />
                )}
              </div>
            ))}
          </div>
        </Spin>
      </div>

      {/* 右侧权限配置 */}
      <div className="config-panel">
        {!selectedRole ? (
          <div className="config-empty">
            <Empty
              description="请在左侧选择一个角色"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          </div>
        ) : (
          <>
            <div className="config-header">
              <div className="config-header-info">
                <Title level={5} style={{ margin: 0 }}>
                  {selectedRole.roleName}
                </Title>
                <Text type="secondary">{selectedRole.roleCode}</Text>
              </div>
            </div>

            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              className="config-tabs"
              items={[
                {
                  key: 'permission',
                  label: (
                    <span>
                      <SafetyOutlined /> 功能权限
                      <Badge
                        count={permCount}
                        size="small"
                        style={{ marginLeft: 6, backgroundColor: permChanged ? '#ff4d4f' : '#52c41a' }}
                      />
                    </span>
                  ),
                  children: (
                    <div className="tree-container">
                      <div className="tree-toolbar">
                        <Space>
                          <Button size="small" onClick={handleSelectAllPerm}>全选</Button>
                          <Button size="small" onClick={handleClearAllPerm}>清空</Button>
                          <Button size="small" icon={<UndoOutlined />} onClick={() => setCheckedPermKeys([...savedPermKeys])} disabled={!permChanged}>
                            撤销
                          </Button>
                        </Space>
                        <Space>
                          <Text type="secondary" className="tree-stat">
                            已选 {permCount}/{totalPermCount}
                          </Text>
                          <Button
                            type="primary"
                            icon={<SaveOutlined />}
                            onClick={handleSavePerm}
                            loading={permSaving}
                            disabled={!permChanged}
                          >
                            保存
                          </Button>
                        </Space>
                      </div>
                      <Spin spinning={permTreeLoading}>
                        {permTreeData.length > 0 ? (
                          <Tree
                            checkable
                            checkStrictly
                            defaultExpandAll
                            checkedKeys={checkedPermKeys}
                            onCheck={(keys) => {
                              const checked = keys.checked || keys;
                              setCheckedPermKeys(Array.isArray(checked) ? checked : []);
                            }}
                            treeData={permTreeData}
                            className="perm-tree"
                            height={500}
                          />
                        ) : (
                          <Empty description="暂无权限数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                        )}
                      </Spin>
                    </div>
                  ),
                },
                {
                  key: 'menu',
                  label: (
                    <span>
                      <MenuOutlined /> 菜单权限
                      <Badge
                        count={menuCount}
                        size="small"
                        style={{ marginLeft: 6, backgroundColor: menuChanged ? '#ff4d4f' : '#52c41a' }}
                      />
                    </span>
                  ),
                  children: (
                    <div className="tree-container">
                      <div className="tree-toolbar">
                        <Space>
                          <Button size="small" onClick={handleSelectAllMenu}>全选</Button>
                          <Button size="small" onClick={handleClearAllMenu}>清空</Button>
                          <Button size="small" icon={<UndoOutlined />} onClick={() => setCheckedMenuKeys([...savedMenuKeys])} disabled={!menuChanged}>
                            撤销
                          </Button>
                        </Space>
                        <Space>
                          <Text type="secondary" className="tree-stat">
                            已选 {menuCount}/{totalMenuCount}
                          </Text>
                          <Button
                            type="primary"
                            icon={<SaveOutlined />}
                            onClick={handleSaveMenu}
                            loading={menuSaving}
                            disabled={!menuChanged}
                          >
                            保存
                          </Button>
                        </Space>
                      </div>
                      <Spin spinning={menuTreeLoading}>
                        {menuTreeData.length > 0 ? (
                          <Tree
                            checkable
                            checkStrictly
                            defaultExpandAll
                            checkedKeys={checkedMenuKeys}
                            onCheck={(keys) => {
                              const checked = keys.checked || keys;
                              setCheckedMenuKeys(Array.isArray(checked) ? checked : []);
                            }}
                            treeData={menuTreeData}
                            className="perm-tree"
                            height={500}
                          />
                        ) : (
                          <Empty description="暂无菜单数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                        )}
                      </Spin>
                    </div>
                  ),
                },
              ]}
            />
          </>
        )}
      </div>
    </div>
  );
}
