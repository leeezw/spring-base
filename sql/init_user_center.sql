-- 用户表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    email VARCHAR(100),
    phone VARCHAR(20),
    status SMALLINT DEFAULT 1, -- 1:正常 0:禁用
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(50) NOT NULL,
    permission_type SMALLINT NOT NULL, -- 1:菜单 2:按钮 3:API
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 菜单表
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    visible SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 部门表
CREATE TABLE sys_dept (
    id BIGSERIAL PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    leader_id BIGINT,
    phone VARCHAR(20),
    email VARCHAR(100),
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);

-- 创建索引
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_role_code ON sys_role(role_code);
CREATE INDEX idx_permission_code ON sys_permission(permission_code);
CREATE INDEX idx_permission_type ON sys_permission(permission_type);
CREATE INDEX idx_menu_parent ON sys_menu(parent_id);
CREATE INDEX idx_dept_parent ON sys_dept(parent_id);
CREATE INDEX idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_user_role_role ON sys_user_role(role_id);
CREATE INDEX idx_role_perm_role ON sys_role_permission(role_id);
CREATE INDEX idx_role_perm_perm ON sys_role_permission(permission_id);
CREATE INDEX idx_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX idx_role_menu_menu ON sys_role_menu(menu_id);

-- 插入初始数据
-- 超级管理员用户
INSERT INTO sys_user (id, username, password, nickname, status) 
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1);
-- 密码: admin123 (BCrypt加密)

-- 超级管理员角色
INSERT INTO sys_role (id, role_code, role_name, description, sort_order) 
VALUES (1, 'ROLE_ADMIN', '超级管理员', '拥有所有权限', 0);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 基础权限
INSERT INTO sys_permission (id, permission_code, permission_name, permission_type, parent_id, sort_order) VALUES
(1, 'system', '系统管理', 1, 0, 1),
(2, 'system:user', '用户管理', 1, 1, 1),
(3, 'system:user:query', '查询用户', 3, 2, 1),
(4, 'system:user:add', '新增用户', 3, 2, 2),
(5, 'system:user:edit', '编辑用户', 3, 2, 3),
(6, 'system:user:delete', '删除用户', 3, 2, 4),
(7, 'system:role', '角色管理', 1, 1, 2),
(8, 'system:role:query', '查询角色', 3, 7, 1),
(9, 'system:role:add', '新增角色', 3, 7, 2),
(10, 'system:role:edit', '编辑角色', 3, 7, 3),
(11, 'system:role:delete', '删除角色', 3, 7, 4);

-- 角色权限关联 (超级管理员拥有所有权限)
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 1, id FROM sys_permission;

-- 基础菜单
INSERT INTO sys_menu (id, menu_name, parent_id, path, component, icon, sort_order) VALUES
(1, '系统管理', 0, '/system', 'Layout', 'setting', 1),
(2, '用户管理', 1, '/system/user', 'system/user/index', 'user', 1),
(3, '角色管理', 1, '/system/role', 'system/role/index', 'team', 2),
(4, '菜单管理', 1, '/system/menu', 'system/menu/index', 'menu', 3),
(5, '部门管理', 1, '/system/dept', 'system/dept/index', 'organization', 4);

-- 角色菜单关联
INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 1, id FROM sys_menu;

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON TABLE sys_permission IS '权限表';
COMMENT ON TABLE sys_menu IS '菜单表';
COMMENT ON TABLE sys_dept IS '部门表';
