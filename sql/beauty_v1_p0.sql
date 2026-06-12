-- =====================================================
-- 悦美 V1 P0 业务表
-- 数据库：PostgreSQL
-- =====================================================

CREATE TABLE IF NOT EXISTS beauty_store
(
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    store_code          VARCHAR(32)  NOT NULL,
    store_name          VARCHAR(100) NOT NULL,
    manager_employee_id BIGINT,
    phone               VARCHAR(20),
    province            VARCHAR(64),
    city                VARCHAR(64),
    district            VARCHAR(64),
    address             VARCHAR(255),
    business_hours      JSONB,
    status              SMALLINT     NOT NULL DEFAULT 1,
    remark              VARCHAR(500),
    create_time         TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_beauty_store_code UNIQUE (tenant_id, store_code)
);

COMMENT ON TABLE beauty_store IS '美业门店表';
COMMENT ON COLUMN beauty_store.store_code IS '门店编码，租户内唯一';
COMMENT ON COLUMN beauty_store.store_name IS '门店名称';
COMMENT ON COLUMN beauty_store.manager_employee_id IS '负责人/店长员工ID';
COMMENT ON COLUMN beauty_store.business_hours IS '营业时间配置JSON';
COMMENT ON COLUMN beauty_store.status IS '营业状态：1营业中 0停业 2装修中';

CREATE INDEX IF NOT EXISTS idx_beauty_store_tenant_status ON beauty_store (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_beauty_store_tenant_name ON beauty_store (tenant_id, store_name);

-- 部门归属门店。部门仍使用原系统部门表，悦美场景下挂在门店下面。
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS store_id BIGINT;

COMMENT ON COLUMN sys_dept.store_id IS '所属门店ID';

CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant_store_status ON sys_dept (tenant_id, store_id, status);

-- 员工档案美业适配字段。V1 一名员工只归属一个主门店。
ALTER TABLE sys_employee ADD COLUMN IF NOT EXISTS store_id BIGINT;
ALTER TABLE sys_employee ADD COLUMN IF NOT EXISTS service_enabled SMALLINT NOT NULL DEFAULT 1;

COMMENT ON COLUMN sys_employee.store_id IS '悦美V1主门店ID';
COMMENT ON COLUMN sys_employee.service_enabled IS '是否可服务：1是 0否';

CREATE INDEX IF NOT EXISTS idx_employee_tenant_store_status ON sys_employee (tenant_id, store_id, status);

CREATE TABLE IF NOT EXISTS beauty_member
(
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    member_no            VARCHAR(32)  NOT NULL,
    name                 VARCHAR(64)  NOT NULL,
    phone                VARCHAR(20)  NOT NULL,
    gender               SMALLINT     NOT NULL DEFAULT 0,
    birthday             DATE,
    source               VARCHAR(32),
    level                VARCHAR(32)  NOT NULL DEFAULT 'normal',
    tags                 JSONB,
    belong_store_id      BIGINT       NOT NULL,
    total_consume_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    last_consume_time    TIMESTAMP,
    status               SMALLINT     NOT NULL DEFAULT 1,
    remark               VARCHAR(500),
    create_time          TIMESTAMP,
    update_time          TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    deleted              SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_beauty_member_no UNIQUE (tenant_id, member_no),
    CONSTRAINT uk_beauty_member_phone UNIQUE (tenant_id, phone)
);

COMMENT ON TABLE beauty_member IS '美业会员档案表';
COMMENT ON COLUMN beauty_member.member_no IS '会员编号，租户内唯一';
COMMENT ON COLUMN beauty_member.phone IS '手机号，租户内唯一';
COMMENT ON COLUMN beauty_member.gender IS '性别：0未知 1男 2女';
COMMENT ON COLUMN beauty_member.level IS '会员等级字典值';
COMMENT ON COLUMN beauty_member.tags IS '会员标签数组';
COMMENT ON COLUMN beauty_member.belong_store_id IS '归属门店ID';
COMMENT ON COLUMN beauty_member.total_consume_amount IS '累计净消费金额';
COMMENT ON COLUMN beauty_member.status IS '会员状态：1正常 0禁用';

CREATE INDEX IF NOT EXISTS idx_beauty_member_tenant_store ON beauty_member (tenant_id, belong_store_id);
CREATE INDEX IF NOT EXISTS idx_beauty_member_tenant_status ON beauty_member (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_beauty_member_tenant_create_time ON beauty_member (tenant_id, create_time);
CREATE INDEX IF NOT EXISTS idx_beauty_member_tags ON beauty_member USING GIN (tags);

-- 悦美业务菜单种子。若目标库菜单ID冲突，可通过菜单管理手动配置同等菜单。
INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, 0, '悦美经营', NULL, NULL, 'tabler-building-store', 20, 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND menu_name = '悦美经营' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '门店管理', '/beauty/store', 'beauty/store/index', 'tabler-building-shop', 10, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0
  AND parent.menu_name = '悦美经营'
  AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/store' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '员工管理', '/beauty/employee', 'beauty/employee/index', 'tabler-users', 20, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0
  AND parent.menu_name = '悦美经营'
  AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/employee' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '会员中心', '/beauty/member', 'beauty/member/index', 'tabler-user-heart', 30, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0
  AND parent.menu_name = '悦美经营'
  AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/member' AND deleted = 0);

-- 悦美业务权限种子
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:store:query', '门店查询', 2, 0, 10, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:store:query' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:store:add', '门店新增', 2, 0, 20, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:store:add' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:store:edit', '门店编辑', 2, 0, 30, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:store:edit' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:store:delete', '门店删除', 2, 0, 40, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:store:delete' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:employee:query', '员工查询', 2, 0, 50, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:employee:query' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:employee:add', '员工新增', 2, 0, 60, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:employee:add' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:employee:edit', '员工编辑', 2, 0, 70, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:employee:edit' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:employee:delete', '员工删除', 2, 0, 80, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:employee:delete' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:member:query', '会员查询', 2, 0, 90, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:member:query' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:member:add', '会员新增', 2, 0, 100, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:member:add' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:member:edit', '会员编辑', 2, 0, 110, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:member:edit' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:member:delete', '会员删除', 2, 0, 120, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:member:delete' AND deleted = 0);

-- 兼容现有 HR 员工接口权限。若库中已存在则不重复写入。
INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'hr:employee:query', 'HR员工查询', 2, 0, 130, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'hr:employee:query' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'hr:employee:add', 'HR员工新增', 2, 0, 140, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'hr:employee:add' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'hr:employee:edit', 'HR员工编辑', 2, 0, 150, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'hr:employee:edit' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'hr:employee:delete', 'HR员工删除', 2, 0, 160, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'hr:employee:delete' AND deleted = 0);

-- 将悦美菜单和门店权限授权给默认超级管理员角色。若实际角色不同，可在角色管理中手动授权。
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.id, m.id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.deleted = 0 AND (m.path IN ('/beauty/store', '/beauty/employee', '/beauty/member') OR m.menu_name = '悦美经营')
WHERE r.deleted = 0
  AND (r.role_code IN ('ADMIN', 'ROLE_ADMIN') OR r.role_code = '*')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.deleted = 0 AND (p.permission_code LIKE 'beauty:%' OR p.permission_code LIKE 'hr:employee:%')
WHERE r.deleted = 0
  AND (r.role_code IN ('ADMIN', 'ROLE_ADMIN') OR r.role_code = '*')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
