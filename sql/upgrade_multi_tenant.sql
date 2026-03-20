-- 租户表
CREATE TABLE sys_tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    expire_time TIMESTAMP,
    account_count INT DEFAULT 0,
    status INT DEFAULT 1,
    logo VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE sys_tenant IS '租户表';
COMMENT ON COLUMN sys_tenant.tenant_code IS '租户编码';
COMMENT ON COLUMN sys_tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN sys_tenant.contact_name IS '联系人';
COMMENT ON COLUMN sys_tenant.contact_phone IS '联系电话';
COMMENT ON COLUMN sys_tenant.contact_email IS '联系邮箱';
COMMENT ON COLUMN sys_tenant.expire_time IS '过期时间';
COMMENT ON COLUMN sys_tenant.account_count IS '账号数量';
COMMENT ON COLUMN sys_tenant.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN sys_tenant.logo IS '租户Logo URL';

-- 给所有业务表添加tenant_id字段
ALTER TABLE sys_user ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_permission ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_menu ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_dept ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- 添加索引
CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_role_tenant ON sys_role(tenant_id);
CREATE INDEX idx_permission_tenant ON sys_permission(tenant_id);
CREATE INDEX idx_menu_tenant ON sys_menu(tenant_id);
CREATE INDEX idx_dept_tenant ON sys_dept(tenant_id);

-- 插入默认租户
INSERT INTO sys_tenant (id, tenant_code, tenant_name, contact_name, status) 
VALUES (1, 'default', '默认租户', '管理员', 1);

-- 更新现有数据的tenant_id
UPDATE sys_user SET tenant_id = 1;
UPDATE sys_role SET tenant_id = 1;
UPDATE sys_permission SET tenant_id = 1;
UPDATE sys_menu SET tenant_id = 1;
UPDATE sys_dept SET tenant_id = 1;
