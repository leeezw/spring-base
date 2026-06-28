-- =====================================================
-- SSL 证书自动化管理模块（scaffold-cert）
-- 数据库：PostgreSQL
-- 说明：ACME 自动签发 / DNS-01 验证 / 自动部署 / 到期续期
-- 多租户：共享库共享表 + tenant_id 字段
-- =====================================================

-- ----------------------------------------------------
-- 1. ACME 账户表
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_acme_account
(
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    ca_type       VARCHAR(32)  NOT NULL DEFAULT 'letsencrypt', -- letsencrypt / zerossl
    email         VARCHAR(200) NOT NULL,
    directory_url VARCHAR(255) NOT NULL,                       -- ACME directory，支持 staging/生产
    account_url   VARCHAR(500),                                -- 注册成功后的账户 location URL
    account_key   TEXT,                                        -- 账户私钥(PEM，AES 加密存储)
    eab_kid       VARCHAR(255),                                -- ZeroSSL EAB kid(加密)
    eab_hmac      VARCHAR(500),                                -- ZeroSSL EAB hmac(加密)
    status        SMALLINT     NOT NULL DEFAULT 1,             -- 1启用 0停用
    remark        VARCHAR(500),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE cert_acme_account IS 'ACME 账户表';
COMMENT ON COLUMN cert_acme_account.ca_type IS 'CA 类型：letsencrypt/zerossl';
COMMENT ON COLUMN cert_acme_account.account_key IS '账户私钥 PEM，AES 加密存储';
COMMENT ON COLUMN cert_acme_account.eab_kid IS 'ZeroSSL EAB kid，加密存储';
CREATE INDEX IF NOT EXISTS idx_cert_acme_account_tenant ON cert_acme_account (tenant_id);

-- ----------------------------------------------------
-- 2. DNS 服务商凭证表（用于 DNS-01 验证自动写 TXT）
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_dns_provider
(
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(32)  NOT NULL,                  -- west/aliyun/dnspod/cloudflare/manual
    credential  TEXT,                                   -- 凭证 JSON，AES 加密存储
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE cert_dns_provider IS 'DNS 服务商凭证表';
COMMENT ON COLUMN cert_dns_provider.type IS 'west西部数码/aliyun/dnspod/cloudflare/manual手动';
COMMENT ON COLUMN cert_dns_provider.credential IS '凭证 JSON，AES 加密存储';
CREATE INDEX IF NOT EXISTS idx_cert_dns_provider_tenant ON cert_dns_provider (tenant_id);

-- ----------------------------------------------------
-- 3. 部署目标表（证书签发后自动部署到的位置）
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_deploy_target
(
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(32)  NOT NULL,                  -- nginx_ssh/west_ssh/baota/qiniu
    config      TEXT,                                   -- 配置 JSON，AES 加密存储
    enabled     SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE cert_deploy_target IS '证书部署目标表';
COMMENT ON COLUMN cert_deploy_target.type IS 'nginx_ssh/west_ssh(SSH)、baota(宝塔API)、qiniu(七牛云)';
COMMENT ON COLUMN cert_deploy_target.config IS '部署配置 JSON，AES 加密存储';
CREATE INDEX IF NOT EXISTS idx_cert_deploy_target_tenant ON cert_deploy_target (tenant_id);

-- ----------------------------------------------------
-- 4. 证书主记录表
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_certificate
(
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    primary_domain   VARCHAR(255) NOT NULL,             -- 主域名
    san_domains      JSONB,                             -- 附加域名(SAN)列表
    challenge_type   VARCHAR(16)  NOT NULL DEFAULT 'dns-01',
    acme_account_id  BIGINT       NOT NULL,
    dns_provider_id  BIGINT,
    key_algo         VARCHAR(16)  NOT NULL DEFAULT 'RSA2048', -- RSA2048 / EC256
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING/ISSUING/ACTIVE/EXPIRING/FAILED
    not_before       TIMESTAMP,
    not_after        TIMESTAMP,
    serial           VARCHAR(128),
    issuer           VARCHAR(255),
    cert_pem         TEXT,                              -- 证书 PEM(加密)
    chain_pem        TEXT,                              -- 证书链 PEM(加密)
    key_pem          TEXT,                              -- 私钥 PEM(加密)
    auto_renew       SMALLINT     NOT NULL DEFAULT 1,
    renew_before_days INT         NOT NULL DEFAULT 30,
    last_renew_at    TIMESTAMP,
    last_message     VARCHAR(1000),
    create_time      TIMESTAMP,
    update_time      TIMESTAMP,
    create_by        BIGINT,
    update_by        BIGINT,
    deleted          SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE cert_certificate IS '证书主记录表';
COMMENT ON COLUMN cert_certificate.san_domains IS '附加域名(SAN)JSON 数组';
COMMENT ON COLUMN cert_certificate.status IS 'PENDING待签/ISSUING签发中/ACTIVE有效/EXPIRING临期/FAILED失败';
COMMENT ON COLUMN cert_certificate.cert_pem IS '证书 PEM，AES 加密存储';
COMMENT ON COLUMN cert_certificate.key_pem IS '私钥 PEM，AES 加密存储';
CREATE INDEX IF NOT EXISTS idx_cert_certificate_tenant ON cert_certificate (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cert_certificate_not_after ON cert_certificate (not_after);

-- ----------------------------------------------------
-- 5. 证书-部署目标关联表
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_certificate_deploy
(
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT      NOT NULL,
    certificate_id     BIGINT      NOT NULL,
    deploy_target_id   BIGINT      NOT NULL,
    last_deploy_status VARCHAR(16),                     -- SUCCESS/FAILED
    last_deploy_time   TIMESTAMP,
    last_message       VARCHAR(1000),
    create_time        TIMESTAMP,
    update_time        TIMESTAMP,
    create_by          BIGINT,
    update_by          BIGINT,
    deleted            SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_cert_deploy UNIQUE (certificate_id, deploy_target_id)
);
COMMENT ON TABLE cert_certificate_deploy IS '证书-部署目标关联表';
CREATE INDEX IF NOT EXISTS idx_cert_cert_deploy_cert ON cert_certificate_deploy (certificate_id);

-- ----------------------------------------------------
-- 6. 任务执行日志表
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS cert_task_log
(
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL,
    certificate_id BIGINT,
    type           VARCHAR(16) NOT NULL,                -- ISSUE/RENEW/DEPLOY/REVOKE
    trigger_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL', -- MANUAL/SCHEDULED
    status         VARCHAR(16) NOT NULL DEFAULT 'RUNNING', -- RUNNING/SUCCESS/FAILED
    message        VARCHAR(1000),
    detail         TEXT,
    started_at     TIMESTAMP,
    finished_at    TIMESTAMP,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    deleted        SMALLINT    NOT NULL DEFAULT 0
);
COMMENT ON TABLE cert_task_log IS '证书任务执行日志表';
COMMENT ON COLUMN cert_task_log.type IS 'ISSUE签发/RENEW续期/DEPLOY部署/REVOKE吊销';
COMMENT ON COLUMN cert_task_log.trigger_source IS 'MANUAL手动/SCHEDULED定时';
CREATE INDEX IF NOT EXISTS idx_cert_task_log_cert ON cert_task_log (certificate_id);
CREATE INDEX IF NOT EXISTS idx_cert_task_log_tenant_time ON cert_task_log (tenant_id, started_at DESC);

-- =====================================================
-- 权限/菜单种子数据（按钮级权限码，供 @RequiresPermissions 校验）
-- permission_type: 1菜单 2按钮 3API
-- =====================================================
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status, tenant_id, create_time, update_time, deleted)
VALUES
  ('cert:certificate:query',  '证书查询', 2, 0, 1, 1, 1, NOW(), NOW(), 0),
  ('cert:certificate:add',    '证书签发', 2, 0, 2, 1, 1, NOW(), NOW(), 0),
  ('cert:certificate:renew',  '证书续期', 2, 0, 3, 1, 1, NOW(), NOW(), 0),
  ('cert:certificate:deploy', '证书部署', 2, 0, 4, 1, 1, NOW(), NOW(), 0),
  ('cert:certificate:delete', '证书删除', 2, 0, 5, 1, 1, NOW(), NOW(), 0),
  ('cert:account:query',      'ACME账户查询', 2, 0, 6, 1, 1, NOW(), NOW(), 0),
  ('cert:account:edit',       'ACME账户管理', 2, 0, 7, 1, 1, NOW(), NOW(), 0),
  ('cert:dns:query',          'DNS服务商查询', 2, 0, 8, 1, 1, NOW(), NOW(), 0),
  ('cert:dns:edit',           'DNS服务商管理', 2, 0, 9, 1, 1, NOW(), NOW(), 0),
  ('cert:deploy:query',       '部署目标查询', 2, 0, 10, 1, 1, NOW(), NOW(), 0),
  ('cert:deploy:edit',        '部署目标管理', 2, 0, 11, 1, 1, NOW(), NOW(), 0),
  ('cert:log:query',          '任务日志查询', 2, 0, 12, 1, 1, NOW(), NOW(), 0)
ON CONFLICT (permission_code) DO NOTHING;

-- 授予超级管理员角色（role_code='ADMIN'）全部证书权限
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code LIKE 'cert:%'
ON CONFLICT (role_id, permission_id) DO NOTHING;
