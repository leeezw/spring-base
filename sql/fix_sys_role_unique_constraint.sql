-- 修复历史约束策略（租户内唯一）：
-- 1) sys_role 由全局 role_code 唯一调整为 (tenant_id, role_code) 唯一
-- 2) sys_user 由全局 username 唯一调整为 (tenant_id, username) 唯一
-- 3) 清理 sys_role_permission 重复唯一约束（uk_role_perm）

ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS sys_role_role_code_key;
ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS uk_sys_role_tenant_code;
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS uk_sys_user_tenant_username;

ALTER TABLE sys_role
    ADD CONSTRAINT uk_sys_role_tenant_code UNIQUE (tenant_id, role_code);
ALTER TABLE sys_user
    ADD CONSTRAINT uk_sys_user_tenant_username UNIQUE (tenant_id, username);

ALTER TABLE sys_role_permission DROP CONSTRAINT IF EXISTS uk_role_perm;

CREATE INDEX IF NOT EXISTS idx_user_tenant_username ON sys_user(tenant_id, username);
CREATE INDEX IF NOT EXISTS idx_role_tenant_code ON sys_role(tenant_id, role_code);
