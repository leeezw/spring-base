-- =============================================
-- 为租户ID=1（默认租户）构建完整的测试数据
-- =============================================

-- 1. 确保租户存在
INSERT INTO sys_tenant (id, tenant_code, tenant_name, contact_name, contact_phone, contact_email, expire_time, account_count, status, create_time, update_time, create_by, update_by, deleted)
VALUES (1, 'default', '默认租户', '管理员', '13800138000', 'admin@example.com', '2030-12-31 23:59:59', 100, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO UPDATE SET
  tenant_name = EXCLUDED.tenant_name,
  contact_name = EXCLUDED.contact_name,
  update_time = NOW();

-- 2. 创建部门（树形结构）
INSERT INTO sys_dept (id, tenant_id, parent_id, dept_code, dept_name, leader, phone, email, sort_order, status, create_time, update_time, create_by, update_by, deleted)
VALUES 
  (1, 1, 0, 'ROOT', '总公司', '张三', '13800138001', 'root@example.com', 0, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, 'TECH', '技术部', '李四', '13800138002', 'tech@example.com', 1, 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 1, 'SALES', '销售部', '王五', '13800138003', 'sales@example.com', 2, 1, NOW(), NOW(), 1, 1, 0),
  (4, 1, 2, 'DEV', '开发组', '赵六', '13800138004', 'dev@example.com', 1, 1, NOW(), NOW(), 1, 1, 0),
  (5, 1, 2, 'TEST', '测试组', '钱七', '13800138005', 'test@example.com', 2, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 3. 创建菜单（树形结构）
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_path, menu_icon, menu_type, sort_order, status, create_time, update_time, create_by, update_by, deleted)
VALUES 
  -- 一级菜单
  (1, 1, 0, '系统管理', '/system', 'IconSettings', 'menu', 1, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 0, '数据分析', '/dashboard', 'IconDashboard', 'menu', 2, 1, NOW(), NOW(), 1, 1, 0),
  
  -- 系统管理子菜单
  (11, 1, 1, '用户管理', '/system/user', 'IconUser', 'menu', 1, 1, NOW(), NOW(), 1, 1, 0),
  (12, 1, 1, '角色管理', '/system/role', 'IconUserGroup', 'menu', 2, 1, NOW(), NOW(), 1, 1, 0),
  (13, 1, 1, '权限管理', '/system/permission', 'IconLock', 'menu', 3, 1, NOW(), NOW(), 1, 1, 0),
  (14, 1, 1, '菜单管理', '/system/menu', 'IconMenu', 'menu', 4, 1, NOW(), NOW(), 1, 1, 0),
  (15, 1, 1, '部门管理', '/system/dept', 'IconBranch', 'menu', 5, 1, NOW(), NOW(), 1, 1, 0),
  (16, 1, 1, '租户管理', '/system/tenant', 'IconApps', 'menu', 6, 1, NOW(), NOW(), 1, 1, 0),
  
  -- 用户管理按钮
  (111, 1, 11, '新增用户', NULL, NULL, 'button', 1, 1, NOW(), NOW(), 1, 1, 0),
  (112, 1, 11, '编辑用户', NULL, NULL, 'button', 2, 1, NOW(), NOW(), 1, 1, 0),
  (113, 1, 11, '删除用户', NULL, NULL, 'button', 3, 1, NOW(), NOW(), 1, 1, 0),
  (114, 1, 11, '查询用户', NULL, NULL, 'button', 4, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 4. 创建权限
INSERT INTO sys_permission (id, tenant_id, permission_code, permission_name, permission_type, remark, status, create_time, update_time, create_by, update_by, deleted)
VALUES 
  -- 系统管理权限
  (1, 1, 'system', '系统管理', 'menu', '系统管理模块', 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 'system:user', '用户管理', 'menu', '用户管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'system:user:query', '查询用户', 'button', '查询用户列表', 1, NOW(), NOW(), 1, 1, 0),
  (4, 1, 'system:user:add', '新增用户', 'button', '新增用户', 1, NOW(), NOW(), 1, 1, 0),
  (5, 1, 'system:user:edit', '编辑用户', 'button', '编辑用户', 1, NOW(), NOW(), 1, 1, 0),
  (6, 1, 'system:user:delete', '删除用户', 'button', '删除用户', 1, NOW(), NOW(), 1, 1, 0),
  
  (7, 1, 'system:role', '角色管理', 'menu', '角色管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (8, 1, 'system:role:query', '查询角色', 'button', '查询角色列表', 1, NOW(), NOW(), 1, 1, 0),
  (9, 1, 'system:role:add', '新增角色', 'button', '新增角色', 1, NOW(), NOW(), 1, 1, 0),
  (10, 1, 'system:role:edit', '编辑角色', 'button', '编辑角色', 1, NOW(), NOW(), 1, 1, 0),
  (11, 1, 'system:role:delete', '删除角色', 'button', '删除角色', 1, NOW(), NOW(), 1, 1, 0),
  
  (12, 1, 'system:permission', '权限管理', 'menu', '权限管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (13, 1, 'system:permission:query', '查询权限', 'button', '查询权限列表', 1, NOW(), NOW(), 1, 1, 0),
  (14, 1, 'system:permission:add', '新增权限', 'button', '新增权限', 1, NOW(), NOW(), 1, 1, 0),
  (15, 1, 'system:permission:edit', '编辑权限', 'button', '编辑权限', 1, NOW(), NOW(), 1, 1, 0),
  (16, 1, 'system:permission:delete', '删除权限', 'button', '删除权限', 1, NOW(), NOW(), 1, 1, 0),
  
  (17, 1, 'system:menu', '菜单管理', 'menu', '菜单管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (18, 1, 'system:menu:query', '查询菜单', 'button', '查询菜单列表', 1, NOW(), NOW(), 1, 1, 0),
  (19, 1, 'system:menu:add', '新增菜单', 'button', '新增菜单', 1, NOW(), NOW(), 1, 1, 0),
  (20, 1, 'system:menu:edit', '编辑菜单', 'button', '编辑菜单', 1, NOW(), NOW(), 1, 1, 0),
  (21, 1, 'system:menu:delete', '删除菜单', 'button', '删除菜单', 1, NOW(), NOW(), 1, 1, 0),
  
  (22, 1, 'system:dept', '部门管理', 'menu', '部门管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (23, 1, 'system:dept:query', '查询部门', 'button', '查询部门列表', 1, NOW(), NOW(), 1, 1, 0),
  (24, 1, 'system:dept:add', '新增部门', 'button', '新增部门', 1, NOW(), NOW(), 1, 1, 0),
  (25, 1, 'system:dept:edit', '编辑部门', 'button', '编辑部门', 1, NOW(), NOW(), 1, 1, 0),
  (26, 1, 'system:dept:delete', '删除部门', 'button', '删除部门', 1, NOW(), NOW(), 1, 1, 0),
  
  (27, 1, 'system:tenant', '租户管理', 'menu', '租户管理菜单', 1, NOW(), NOW(), 1, 1, 0),
  (28, 1, 'system:tenant:query', '查询租户', 'button', '查询租户列表', 1, NOW(), NOW(), 1, 1, 0),
  (29, 1, 'system:tenant:add', '新增租户', 'button', '新增租户', 1, NOW(), NOW(), 1, 1, 0),
  (30, 1, 'system:tenant:edit', '编辑租户', 'button', '编辑租户', 1, NOW(), NOW(), 1, 1, 0),
  (31, 1, 'system:tenant:delete', '删除租户', 'button', '删除租户', 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- 5. 创建角色
INSERT INTO sys_role (id, tenant_id, role_code, role_name, remark, status, create_time, update_time, create_by, update_by, deleted)
VALUES 
  (1, 1, 'ADMIN', '超级管理员', '拥有所有权限', 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 'MANAGER', '部门经理', '部门管理权限', 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'USER', '普通用户', '基础查询权限', 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 6. 角色-权限关联（超级管理员拥有所有权限）
INSERT INTO sys_role_permission (role_id, permission_id, create_time, create_by)
SELECT 1, id, NOW(), 1
FROM sys_permission
WHERE tenant_id = 1 AND deleted = 0
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 7. 角色-菜单关联（超级管理员拥有所有菜单）
INSERT INTO sys_role_menu (role_id, menu_id, create_time, create_by)
SELECT 1, id, NOW(), 1
FROM sys_menu
WHERE tenant_id = 1 AND deleted = 0
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 8. 部门经理角色权限（查询+编辑权限）
INSERT INTO sys_role_permission (role_id, permission_id, create_time, create_by)
SELECT 2, id, NOW(), 1
FROM sys_permission
WHERE tenant_id = 1 
  AND deleted = 0
  AND (permission_code LIKE '%:query' OR permission_code LIKE '%:edit' OR permission_type = 'menu')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 9. 普通用户角色权限（仅查询权限）
INSERT INTO sys_role_permission (role_id, permission_id, create_time, create_by)
SELECT 3, id, NOW(), 1
FROM sys_permission
WHERE tenant_id = 1 
  AND deleted = 0
  AND (permission_code LIKE '%:query' OR permission_type = 'menu')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 10. 确保admin用户存在并关联到超级管理员角色
UPDATE sys_user SET tenant_id = 1 WHERE id = 1;

INSERT INTO sys_user_role (user_id, role_id, create_time, create_by)
VALUES (1, 1, NOW(), 1)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 11. 创建测试用户
INSERT INTO sys_user (id, tenant_id, username, password, nickname, avatar, email, phone, status, create_time, update_time, create_by, update_by, deleted)
VALUES 
  (2, 1, 'manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '部门经理', NULL, 'manager@example.com', '13800138010', 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'user001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通用户', NULL, 'user001@example.com', '13800138011', 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 12. 用户-角色关联
INSERT INTO sys_user_role (user_id, role_id, create_time, create_by)
VALUES 
  (2, 2, NOW(), 1),  -- manager -> 部门经理
  (3, 3, NOW(), 1)   -- user001 -> 普通用户
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 查看结果
SELECT '=== 租户信息 ===' as info;
SELECT * FROM sys_tenant WHERE id = 1;

SELECT '=== 部门信息 ===' as info;
SELECT * FROM sys_dept WHERE tenant_id = 1 ORDER BY sort_order;

SELECT '=== 角色信息 ===' as info;
SELECT * FROM sys_role WHERE tenant_id = 1;

SELECT '=== 用户信息 ===' as info;
SELECT id, username, nickname, email, phone FROM sys_user WHERE tenant_id = 1;

SELECT '=== 权限统计 ===' as info;
SELECT COUNT(*) as total_permissions FROM sys_permission WHERE tenant_id = 1;

SELECT '=== 菜单统计 ===' as info;
SELECT COUNT(*) as total_menus FROM sys_menu WHERE tenant_id = 1;

SELECT '=== 超级管理员权限数 ===' as info;
SELECT COUNT(*) as admin_permissions FROM sys_role_permission WHERE role_id = 1;

SELECT '=== 超级管理员菜单数 ===' as info;
SELECT COUNT(*) as admin_menus FROM sys_role_menu WHERE role_id = 1;
