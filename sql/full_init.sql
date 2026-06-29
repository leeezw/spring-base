-- =====================================================================
-- 整库初始化脚本（全新 PostgreSQL 16）
-- 用途：服务器/数据库迁移时，在一个全新空库上一次性重建完整表结构 + 初始管理数据。
--
-- ⚠️ 重要：本脚本只重建「表结构 + 初始管理员/角色/权限/菜单」等初始数据，
--          不包含、也无法恢复你丢失的真实业务数据（已签发证书、会员、业务记录等）。
--
-- 用法：
--   createdb scaffold_db    # 或用已建好的空库
--   psql "postgresql://用户:密码@主机:5432/scaffold_db" -f sql/full_init.sql
--
-- 初始登录：用户名 admin  密码 admin123  （租户 default）
--
-- 本文件由仓库内零散脚本合并 + 从实体类反推缺失表 DDL 整理而来，幂等可重复执行。
-- =====================================================================

-- =====================================================================
-- 第 1 部分：核心 user-center 表（已内联 tenant_id 与最终列）
-- =====================================================================

-- 租户表
CREATE TABLE IF NOT EXISTS sys_tenant (
    id            BIGSERIAL PRIMARY KEY,
    tenant_code   VARCHAR(50)  NOT NULL UNIQUE,
    tenant_name   VARCHAR(100) NOT NULL,
    contact_name  VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    expire_time   TIMESTAMP,
    account_count INT DEFAULT 0,
    status        INT DEFAULT 1,
    logo          VARCHAR(500),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    deleted       INT DEFAULT 0
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(500),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    status      SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT DEFAULT 0,
    CONSTRAINT uk_sys_user_tenant_username UNIQUE (tenant_id, username)
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL DEFAULT 1,
    role_code   VARCHAR(50) NOT NULL,
    role_name   VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order  INT DEFAULT 0,
    status      SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT DEFAULT 0,
    CONSTRAINT uk_sys_role_tenant_code UNIQUE (tenant_id, role_code)
);

-- 权限表（同时保留 permission_code 单列唯一 + (tenant_id,permission_code) 组合唯一）
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(50)  NOT NULL,
    permission_type SMALLINT     NOT NULL,
    parent_id       BIGINT DEFAULT 0,
    path            VARCHAR(200),
    component       VARCHAR(200),
    icon            VARCHAR(100),
    sort_order      INT DEFAULT 0,
    status          SMALLINT DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         SMALLINT DEFAULT 0,
    CONSTRAINT uk_sys_permission_code UNIQUE (permission_code),
    CONSTRAINT uk_sys_permission_tenant_code UNIQUE (tenant_id, permission_code)
);

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL DEFAULT 1,
    menu_name   VARCHAR(50) NOT NULL,
    parent_id   BIGINT DEFAULT 0,
    path        VARCHAR(200),
    component   VARCHAR(200),
    icon        VARCHAR(100),
    sort_order  INT DEFAULT 0,
    visible     SMALLINT DEFAULT 1,
    status      SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT DEFAULT 0
);

-- 部门表（含美业 store_id）
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL DEFAULT 1,
    dept_name   VARCHAR(50) NOT NULL,
    parent_id   BIGINT DEFAULT 0,
    leader_id   BIGINT,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    sort_order  INT DEFAULT 0,
    status      SMALLINT DEFAULT 1,
    store_id    BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT DEFAULT 0
);

-- 用户-角色 / 角色-权限 / 角色-菜单 关联表（全局共享，无 tenant_id）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGSERIAL PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (role_id, menu_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_user_status ON sys_user(status);
CREATE INDEX IF NOT EXISTS idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_code ON sys_role(role_code);
CREATE INDEX IF NOT EXISTS idx_role_tenant ON sys_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permission_tenant ON sys_permission(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permission_type ON sys_permission(permission_type);
CREATE INDEX IF NOT EXISTS idx_menu_parent ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_menu_tenant ON sys_menu(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dept_parent ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_dept_tenant ON sys_dept(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_role_perm_role ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_role ON sys_role_menu(role_id);

-- =====================================================================
-- 第 2 部分：仓库缺 DDL、从实体类反推的表
-- （sys_login_log/operation_log/post/user_post/role_dept/dict/dict_item/file/position/gen_table*）
-- =====================================================================

-- 登录日志
CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50),
    ip         VARCHAR(64),
    user_agent VARCHAR(512),
    status     SMALLINT,
    message    VARCHAR(500),
    login_time TIMESTAMP,
    tenant_id  BIGINT
);
CREATE INDEX IF NOT EXISTS idx_login_log_username ON sys_login_log(username);
CREATE INDEX IF NOT EXISTS idx_login_log_time ON sys_login_log(login_time);

-- 操作日志
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id             BIGSERIAL PRIMARY KEY,
    module         VARCHAR(100),
    operation_type VARCHAR(32),
    description    VARCHAR(500),
    method         VARCHAR(255),
    request_url    VARCHAR(500),
    request_method VARCHAR(16),
    request_params TEXT,
    response_data  TEXT,
    status         SMALLINT,
    error_msg      TEXT,
    ip             VARCHAR(64),
    user_agent     VARCHAR(512),
    duration       BIGINT,
    user_id        BIGINT,
    username       VARCHAR(50),
    tenant_id      BIGINT,
    create_time    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oper_log_user ON sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_oper_log_time ON sys_operation_log(create_time);

-- 岗位表
CREATE TABLE IF NOT EXISTS sys_post (
    id            BIGSERIAL PRIMARY KEY,
    post_code     VARCHAR(64),
    post_name     VARCHAR(100),
    post_category SMALLINT,
    dept_id       BIGINT,
    sort_order    INT DEFAULT 0,
    status        SMALLINT DEFAULT 1,
    description   VARCHAR(500),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    deleted       SMALLINT DEFAULT 0,
    tenant_id     BIGINT
);
CREATE INDEX IF NOT EXISTS idx_post_tenant ON sys_post(tenant_id);

-- 用户-岗位关联
CREATE TABLE IF NOT EXISTS sys_user_post (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    UNIQUE (user_id, post_id)
);

-- 角色-部门关联（数据权限，复合主键，无独立 id）
CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id)
);

-- 职位表（原 sys_position.sql 为 MySQL 方言，此处重写为 PostgreSQL）
CREATE TABLE IF NOT EXISTS sys_position (
    id             BIGSERIAL PRIMARY KEY,
    post_id        BIGINT,
    position_code  VARCHAR(50),
    position_name  VARCHAR(100),
    position_level INT,
    sort_order     INT DEFAULT 0,
    status         INT DEFAULT 1,
    description    VARCHAR(500),
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    deleted        INT DEFAULT 0,
    tenant_id      BIGINT
);
CREATE INDEX IF NOT EXISTS idx_position_post ON sys_position(post_id);
CREATE INDEX IF NOT EXISTS idx_position_tenant ON sys_position(tenant_id);

-- 数据字典
CREATE TABLE IF NOT EXISTS sys_dict (
    id          BIGSERIAL PRIMARY KEY,
    dict_code   VARCHAR(100),
    dict_name   VARCHAR(100),
    description VARCHAR(500),
    status      SMALLINT DEFAULT 1,
    sort_order  INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT DEFAULT 0,
    tenant_id   BIGINT
);
CREATE INDEX IF NOT EXISTS idx_dict_tenant ON sys_dict(tenant_id);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGSERIAL PRIMARY KEY,
    dict_id     BIGINT,
    item_value  VARCHAR(100),
    item_label  VARCHAR(100),
    item_color  VARCHAR(32),
    item_icon   VARCHAR(100),
    description VARCHAR(500),
    sort_order  INT DEFAULT 0,
    status      SMALLINT DEFAULT 1,
    is_default  SMALLINT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     SMALLINT DEFAULT 0,
    tenant_id   BIGINT
);
CREATE INDEX IF NOT EXISTS idx_dict_item_dict ON sys_dict_item(dict_id);

-- 文件表
CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGSERIAL PRIMARY KEY,
    file_name     VARCHAR(255),
    original_name VARCHAR(255),
    file_path     VARCHAR(1000),
    file_size     BIGINT,
    file_type     VARCHAR(64),
    mime_type     VARCHAR(128),
    storage_type  VARCHAR(32),
    module        VARCHAR(64),
    create_time   TIMESTAMP,
    create_by     BIGINT,
    tenant_id     BIGINT,
    deleted       SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_file_tenant ON sys_file(tenant_id);

-- 代码生成器表
CREATE TABLE IF NOT EXISTS gen_table (
    id            BIGSERIAL PRIMARY KEY,
    table_name    VARCHAR(128),
    table_comment VARCHAR(255),
    class_name    VARCHAR(128),
    package_name  VARCHAR(255),
    module_name   VARCHAR(64),
    business_name VARCHAR(64),
    function_name VARCHAR(64),
    author        VARCHAR(64),
    gen_type      VARCHAR(16),
    gen_path      VARCHAR(255),
    options       TEXT,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gen_table_column (
    id             BIGSERIAL PRIMARY KEY,
    table_id       BIGINT,
    column_name    VARCHAR(128),
    column_comment VARCHAR(255),
    column_type    VARCHAR(64),
    java_type      VARCHAR(64),
    java_field     VARCHAR(64),
    is_pk          BOOLEAN,
    is_increment   BOOLEAN,
    is_required    BOOLEAN,
    is_insert      BOOLEAN,
    is_edit        BOOLEAN,
    is_list        BOOLEAN,
    is_query       BOOLEAN,
    query_type     VARCHAR(32),
    html_type      VARCHAR(32),
    dict_type      VARCHAR(64),
    sort_order     INT DEFAULT 0,
    create_time    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_gen_column_table ON gen_table_column(table_id);

-- =====================================================================
-- 第 3 部分：员工档案模块（来自 sys_employee.sql，已内联美业/职位扩展列）
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_employee (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    user_id             BIGINT,
    dept_id             BIGINT,
    post_id             BIGINT,
    position_id         BIGINT,
    store_id            BIGINT,
    service_enabled     SMALLINT     NOT NULL DEFAULT 1,
    emp_code            VARCHAR(32)  NOT NULL,
    emp_name            VARCHAR(64)  NOT NULL,
    gender              SMALLINT     DEFAULT 0,
    phone               VARCHAR(20),
    email               VARCHAR(128),
    avatar              VARCHAR(512),
    id_card             VARCHAR(18),
    birthday            DATE,
    nation              VARCHAR(32),
    native_place        VARCHAR(128),
    address             VARCHAR(256),
    hire_date           DATE,
    emp_type            SMALLINT     DEFAULT 1,
    probation_end_date  DATE,
    status              SMALLINT     NOT NULL DEFAULT 1,
    emergency_contact   VARCHAR(64),
    emergency_phone     VARCHAR(20),
    emergency_relation  VARCHAR(32),
    custom_fields       JSONB,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_employee_emp_code UNIQUE (emp_code, tenant_id)
);
CREATE INDEX IF NOT EXISTS idx_employee_custom_fields ON sys_employee USING GIN (custom_fields);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_dept ON sys_employee (tenant_id, dept_id);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_status ON sys_employee (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_store_status ON sys_employee (tenant_id, store_id, status);

CREATE TABLE IF NOT EXISTS sys_employee_field (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    field_key   VARCHAR(64) NOT NULL,
    field_label VARCHAR(64) NOT NULL,
    field_type  VARCHAR(16) NOT NULL,
    options     JSONB,
    required    SMALLINT    DEFAULT 0,
    sort_order  INTEGER     DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_employee_field_key UNIQUE (field_key, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant_store_status ON sys_dept (tenant_id, store_id, status);

-- =====================================================================
-- 第 4 部分：美业模块表（来自 beauty_*.sql）
-- =====================================================================
CREATE TABLE IF NOT EXISTS beauty_store (
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
CREATE INDEX IF NOT EXISTS idx_beauty_store_tenant_status ON beauty_store (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_beauty_store_tenant_name ON beauty_store (tenant_id, store_name);

CREATE TABLE IF NOT EXISTS beauty_member (
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
CREATE INDEX IF NOT EXISTS idx_beauty_member_tenant_store ON beauty_member (tenant_id, belong_store_id);
CREATE INDEX IF NOT EXISTS idx_beauty_member_tenant_status ON beauty_member (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_beauty_member_tags ON beauty_member USING GIN (tags);

CREATE TABLE IF NOT EXISTS beauty_appointment (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    appointment_no    VARCHAR(32)  NOT NULL,
    member_id         BIGINT       NOT NULL,
    store_id          BIGINT       NOT NULL,
    service_item_id   BIGINT,
    service_item_name VARCHAR(100) NOT NULL,
    employee_id       BIGINT,
    start_time        TIMESTAMP    NOT NULL,
    end_time          TIMESTAMP    NOT NULL,
    status            SMALLINT     NOT NULL DEFAULT 0,
    source            VARCHAR(32),
    cancel_reason     VARCHAR(255),
    no_show_reason    VARCHAR(255),
    remark            VARCHAR(500),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT,
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_beauty_appointment_no UNIQUE (tenant_id, appointment_no)
);
CREATE INDEX IF NOT EXISTS idx_beauty_appointment_store_time ON beauty_appointment (tenant_id, store_id, start_time, status);
CREATE INDEX IF NOT EXISTS idx_beauty_appointment_member ON beauty_appointment (tenant_id, member_id, start_time);

CREATE TABLE IF NOT EXISTS beauty_appointment_log (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL,
    appointment_id BIGINT      NOT NULL,
    action         VARCHAR(32) NOT NULL,
    from_status    SMALLINT,
    to_status      SMALLINT,
    operator_id    BIGINT,
    reason         VARCHAR(255),
    old_start_time TIMESTAMP,
    old_end_time   TIMESTAMP,
    new_start_time TIMESTAMP,
    new_end_time   TIMESTAMP,
    remark         VARCHAR(500),
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    deleted        SMALLINT    NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_beauty_appointment_log_appointment ON beauty_appointment_log (tenant_id, appointment_id, create_time);

CREATE TABLE IF NOT EXISTS beauty_member_follow_record (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    member_id        BIGINT       NOT NULL,
    store_id         BIGINT,
    follow_type      VARCHAR(32),
    follow_result    VARCHAR(32),
    content          VARCHAR(1000) NOT NULL,
    next_follow_time TIMESTAMP,
    operator_id      BIGINT,
    create_time      TIMESTAMP,
    update_time      TIMESTAMP,
    create_by        BIGINT,
    update_by        BIGINT,
    deleted          SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_beauty_member_follow_member ON beauty_member_follow_record (tenant_id, member_id, create_time);

-- =====================================================================
-- 第 5 部分：证书自动化模块表（来自 cert_automation.sql）
-- =====================================================================
CREATE TABLE IF NOT EXISTS cert_acme_account (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    ca_type       VARCHAR(32)  NOT NULL DEFAULT 'letsencrypt',
    email         VARCHAR(200) NOT NULL,
    directory_url VARCHAR(255) NOT NULL,
    account_url   VARCHAR(500),
    account_key   TEXT,
    eab_kid       VARCHAR(255),
    eab_hmac      VARCHAR(500),
    status        SMALLINT     NOT NULL DEFAULT 1,
    remark        VARCHAR(500),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cert_acme_account_tenant ON cert_acme_account (tenant_id);

CREATE TABLE IF NOT EXISTS cert_dns_provider (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    credential  TEXT,
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cert_dns_provider_tenant ON cert_dns_provider (tenant_id);

CREATE TABLE IF NOT EXISTS cert_deploy_target (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    config      TEXT,
    enabled     SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cert_deploy_target_tenant ON cert_deploy_target (tenant_id);

CREATE TABLE IF NOT EXISTS cert_certificate (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    primary_domain    VARCHAR(255) NOT NULL,
    san_domains       JSONB,
    challenge_type    VARCHAR(16)  NOT NULL DEFAULT 'dns-01',
    acme_account_id   BIGINT       NOT NULL,
    dns_provider_id   BIGINT,
    key_algo          VARCHAR(16)  NOT NULL DEFAULT 'RSA2048',
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    not_before        TIMESTAMP,
    not_after         TIMESTAMP,
    serial            VARCHAR(128),
    issuer            VARCHAR(255),
    cert_pem          TEXT,
    chain_pem         TEXT,
    key_pem           TEXT,
    auto_renew        SMALLINT     NOT NULL DEFAULT 1,
    renew_before_days INT          NOT NULL DEFAULT 30,
    last_renew_at     TIMESTAMP,
    last_message      VARCHAR(1000),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT,
    deleted           SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cert_certificate_tenant ON cert_certificate (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cert_certificate_not_after ON cert_certificate (not_after);

CREATE TABLE IF NOT EXISTS cert_certificate_deploy (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT      NOT NULL,
    certificate_id     BIGINT      NOT NULL,
    deploy_target_id   BIGINT      NOT NULL,
    last_deploy_status VARCHAR(16),
    last_deploy_time   TIMESTAMP,
    last_message       VARCHAR(1000),
    create_time        TIMESTAMP,
    update_time        TIMESTAMP,
    create_by          BIGINT,
    update_by          BIGINT,
    deleted            SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_cert_deploy UNIQUE (certificate_id, deploy_target_id)
);
CREATE INDEX IF NOT EXISTS idx_cert_cert_deploy_cert ON cert_certificate_deploy (certificate_id);

CREATE TABLE IF NOT EXISTS cert_task_log (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL,
    certificate_id BIGINT,
    type           VARCHAR(16) NOT NULL,
    trigger_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    status         VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
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
CREATE INDEX IF NOT EXISTS idx_cert_task_log_cert ON cert_task_log (certificate_id);
CREATE INDEX IF NOT EXISTS idx_cert_task_log_tenant_time ON cert_task_log (tenant_id, started_at DESC);

-- =====================================================================
-- 第 6 部分：核心初始数据（租户 / 部门 / 菜单 / 权限 / 角色 / 用户）
-- 权威来源：insert_test_data_fixed.sql（已去掉末尾 SELECT 报表）
-- 初始账号：admin/admin123、manager/admin123、user001/admin123
-- =====================================================================
INSERT INTO sys_tenant (id, tenant_code, tenant_name, contact_name, contact_phone, contact_email, expire_time, account_count, status, create_time, update_time, create_by, update_by, deleted)
VALUES (1, 'default', '默认租户', '管理员', '13800138000', 'admin@example.com', '2030-12-31 23:59:59', 100, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO UPDATE SET tenant_name = EXCLUDED.tenant_name, update_time = NOW();

INSERT INTO sys_dept (id, tenant_id, parent_id, dept_name, leader_id, phone, email, sort_order, status, create_time, update_time, create_by, update_by, deleted)
VALUES
  (1, 1, 0, '总公司', NULL, '13800138001', 'root@example.com', 0, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, '技术部', NULL, '13800138002', 'tech@example.com', 1, 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 1, '销售部', NULL, '13800138003', 'sales@example.com', 2, 1, NOW(), NOW(), 1, 1, 0),
  (4, 1, 2, '开发组', NULL, '13800138004', 'dev@example.com', 1, 1, NOW(), NOW(), 1, 1, 0),
  (5, 1, 2, '测试组', NULL, '13800138005', 'test@example.com', 2, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, create_by, update_by, deleted)
VALUES
  (1, 1, 0, '系统管理', '/system', 'Layout', 'IconSettings', 1, 1, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 0, '数据分析', '/dashboard', 'Layout', 'IconDashboard', 2, 1, 1, NOW(), NOW(), 1, 1, 0),
  (11, 1, 1, '用户管理', '/system/user', 'system/user/index', 'IconUser', 1, 1, 1, NOW(), NOW(), 1, 1, 0),
  (12, 1, 1, '角色管理', '/system/role', 'system/role/index', 'IconUserGroup', 2, 1, 1, NOW(), NOW(), 1, 1, 0),
  (13, 1, 1, '权限管理', '/system/permission', 'system/permission/index', 'IconLock', 3, 1, 1, NOW(), NOW(), 1, 1, 0),
  (14, 1, 1, '菜单管理', '/system/menu', 'system/menu/index', 'IconMenu', 4, 1, 1, NOW(), NOW(), 1, 1, 0),
  (15, 1, 1, '部门管理', '/system/dept', 'system/dept/index', 'IconBranch', 5, 1, 1, NOW(), NOW(), 1, 1, 0),
  (16, 1, 1, '租户管理', '/system/tenant', 'system/tenant/index', 'IconApps', 6, 1, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_permission (id, tenant_id, permission_code, permission_name, permission_type, parent_id, path, component, icon, sort_order, status, create_time, update_time, create_by, update_by, deleted)
VALUES
  (1, 1, 'system', '系统管理', 1, 0, '/system', NULL, 'IconSettings', 1, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 'system:user', '用户管理', 1, 1, '/system/user', NULL, 'IconUser', 1, 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'system:user:query', '查询用户', 2, 2, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (4, 1, 'system:user:add', '新增用户', 2, 2, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (5, 1, 'system:user:edit', '编辑用户', 2, 2, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (6, 1, 'system:user:delete', '删除用户', 2, 2, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0),
  (7, 1, 'system:role', '角色管理', 1, 1, '/system/role', NULL, 'IconUserGroup', 2, 1, NOW(), NOW(), 1, 1, 0),
  (8, 1, 'system:role:query', '查询角色', 2, 7, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (9, 1, 'system:role:add', '新增角色', 2, 7, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (10, 1, 'system:role:edit', '编辑角色', 2, 7, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (11, 1, 'system:role:delete', '删除角色', 2, 7, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0),
  (12, 1, 'system:permission', '权限管理', 1, 1, '/system/permission', NULL, 'IconLock', 3, 1, NOW(), NOW(), 1, 1, 0),
  (13, 1, 'system:permission:query', '查询权限', 2, 12, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (14, 1, 'system:permission:add', '新增权限', 2, 12, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (15, 1, 'system:permission:edit', '编辑权限', 2, 12, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (16, 1, 'system:permission:delete', '删除权限', 2, 12, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0),
  (17, 1, 'system:menu', '菜单管理', 1, 1, '/system/menu', NULL, 'IconMenu', 4, 1, NOW(), NOW(), 1, 1, 0),
  (18, 1, 'system:menu:query', '查询菜单', 2, 17, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (19, 1, 'system:menu:add', '新增菜单', 2, 17, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (20, 1, 'system:menu:edit', '编辑菜单', 2, 17, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (21, 1, 'system:menu:delete', '删除菜单', 2, 17, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0),
  (22, 1, 'system:dept', '部门管理', 1, 1, '/system/dept', NULL, 'IconBranch', 5, 1, NOW(), NOW(), 1, 1, 0),
  (23, 1, 'system:dept:query', '查询部门', 2, 22, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (24, 1, 'system:dept:add', '新增部门', 2, 22, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (25, 1, 'system:dept:edit', '编辑部门', 2, 22, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (26, 1, 'system:dept:delete', '删除部门', 2, 22, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0),
  (27, 1, 'system:tenant', '租户管理', 1, 1, '/system/tenant', NULL, 'IconApps', 6, 1, NOW(), NOW(), 1, 1, 0),
  (28, 1, 'system:tenant:query', '查询租户', 2, 27, NULL, NULL, NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (29, 1, 'system:tenant:add', '新增租户', 2, 27, NULL, NULL, NULL, 2, 1, NOW(), NOW(), 1, 1, 0),
  (30, 1, 'system:tenant:edit', '编辑租户', 2, 27, NULL, NULL, NULL, 3, 1, NOW(), NOW(), 1, 1, 0),
  (31, 1, 'system:tenant:delete', '删除租户', 2, 27, NULL, NULL, NULL, 4, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

INSERT INTO sys_role (id, tenant_id, role_code, role_name, description, sort_order, status, create_time, update_time, create_by, update_by, deleted)
VALUES
  (1, 1, 'ADMIN', '超级管理员', '拥有所有权限', 0, 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 'MANAGER', '部门经理', '部门管理权限', 1, 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'USER', '普通用户', '基础查询权限', 2, 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO UPDATE SET role_name = EXCLUDED.role_name, update_time = NOW();

INSERT INTO sys_user (id, tenant_id, username, password, nickname, avatar, email, phone, status, create_time, update_time, create_by, update_by, deleted)
VALUES
  (1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', NULL, 'admin@example.com', '13800138000', 1, NOW(), NOW(), 1, 1, 0),
  (2, 1, 'manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '部门经理', NULL, 'manager@example.com', '13800138010', 1, NOW(), NOW(), 1, 1, 0),
  (3, 1, 'user001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通用户', NULL, 'user001@example.com', '13800138011', 1, NOW(), NOW(), 1, 1, 0)
ON CONFLICT (id) DO UPDATE SET nickname = EXCLUDED.nickname, update_time = NOW();

-- 超级管理员拥有全部权限/菜单
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT 1, id, NOW() FROM sys_permission WHERE tenant_id = 1 AND deleted = 0
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW() FROM sys_menu WHERE tenant_id = 1 AND deleted = 0
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 部门经理：查询+编辑+菜单
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT 2, id, NOW() FROM sys_permission
WHERE tenant_id = 1 AND deleted = 0
  AND (permission_code LIKE '%:query' OR permission_code LIKE '%:edit' OR permission_type = 1)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 普通用户：查询+菜单
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT 3, id, NOW() FROM sys_permission
WHERE tenant_id = 1 AND deleted = 0
  AND (permission_code LIKE '%:query' OR permission_type = 1)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 用户-角色
INSERT INTO sys_user_role (user_id, role_id, create_time)
VALUES (1, 1, NOW()), (2, 2, NOW()), (3, 3, NOW())
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =====================================================================
-- 第 7 部分：重置序列（关键！）
-- 上面用显式 id 插入了数据，序列仍停在初值；不重置则应用自增插入会主键冲突。
-- 必须在后续“按自增插入”的 beauty/cert 种子之前执行。
-- =====================================================================
SELECT setval(pg_get_serial_sequence('sys_tenant', 'id'),     GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_tenant), 1));
SELECT setval(pg_get_serial_sequence('sys_dept', 'id'),       GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_dept), 1));
SELECT setval(pg_get_serial_sequence('sys_menu', 'id'),       GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_menu), 1));
SELECT setval(pg_get_serial_sequence('sys_permission', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_permission), 1));
SELECT setval(pg_get_serial_sequence('sys_role', 'id'),       GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_role), 1));
SELECT setval(pg_get_serial_sequence('sys_user', 'id'),       GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_user), 1));

-- =====================================================================
-- 第 8 部分：美业模块菜单/权限/授权种子（来自 beauty_*.sql，tenant_id=0 全局菜单）
-- =====================================================================
INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, 0, '悦美经营', NULL, NULL, 'tabler-building-store', 20, 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND menu_name = '悦美经营' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '门店管理', '/beauty/store', 'beauty/store/index', 'tabler-building-shop', 10, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0 AND parent.menu_name = '悦美经营' AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/store' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '员工管理', '/beauty/employee', 'beauty/employee/index', 'tabler-users', 20, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0 AND parent.menu_name = '悦美经营' AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/employee' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '会员中心', '/beauty/member', 'beauty/member/index', 'tabler-user-heart', 30, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0 AND parent.menu_name = '悦美经营' AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/member' AND deleted = 0);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '预约中心', '/beauty/appointment', 'beauty/appointment/index', 'tabler-calendar-time', 40, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0 AND parent.menu_name = '悦美经营' AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/appointment' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, v.code, v.name, 2, 0, v.so, 1, NOW(), NOW(), 0
FROM (VALUES
  ('beauty:store:query','门店查询',10),('beauty:store:add','门店新增',20),('beauty:store:edit','门店编辑',30),('beauty:store:delete','门店删除',40),
  ('beauty:employee:query','员工查询',50),('beauty:employee:add','员工新增',60),('beauty:employee:edit','员工编辑',70),('beauty:employee:delete','员工删除',80),
  ('beauty:member:query','会员查询',90),('beauty:member:add','会员新增',100),('beauty:member:edit','会员编辑',110),('beauty:member:delete','会员删除',120),
  ('hr:employee:query','HR员工查询',130),('hr:employee:add','HR员工新增',140),('hr:employee:edit','HR员工编辑',150),('hr:employee:delete','HR员工删除',160),
  ('beauty:appointment:query','预约查询',170),('beauty:appointment:add','预约新增',180),('beauty:appointment:edit','预约编辑',190),('beauty:appointment:delete','预约删除',200)
) AS v(code, name, so)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 美业菜单/权限授权给超级管理员
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.id, m.id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.deleted = 0 AND (m.path IN ('/beauty/store','/beauty/employee','/beauty/member','/beauty/appointment') OR m.menu_name = '悦美经营')
WHERE r.deleted = 0 AND r.role_code IN ('ADMIN','ROLE_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.deleted = 0 AND (p.permission_code LIKE 'beauty:%' OR p.permission_code LIKE 'hr:employee:%')
WHERE r.deleted = 0 AND r.role_code IN ('ADMIN','ROLE_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- =====================================================================
-- 第 9 部分：证书模块权限/授权种子（来自 cert_automation.sql）
-- =====================================================================
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status, tenant_id, create_time, update_time, deleted)
SELECT v.permission_code, v.permission_name, 2, 0, v.sort_order, 1, 1, NOW(), NOW(), 0
FROM (VALUES
  ('cert:certificate:query',  '证书查询', 1),
  ('cert:certificate:add',    '证书签发', 2),
  ('cert:certificate:renew',  '证书续期', 3),
  ('cert:certificate:deploy', '证书部署', 4),
  ('cert:certificate:delete', '证书删除', 5),
  ('cert:account:query',      'ACME账户查询', 6),
  ('cert:account:edit',       'ACME账户管理', 7),
  ('cert:dns:query',          'DNS服务商查询', 8),
  ('cert:dns:edit',           'DNS服务商管理', 9),
  ('cert:deploy:query',       '部署目标查询', 10),
  ('cert:deploy:edit',        '部署目标管理', 11),
  ('cert:log:query',          '任务日志查询', 12)
) AS v(permission_code, permission_name, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.permission_code AND p.tenant_id = 1);

INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('ADMIN','ROLE_ADMIN')
  AND p.permission_code LIKE 'cert:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- =====================================================================
-- 完成。建议执行后用 \dt 核对表数量，并用 admin / admin123 登录验证。
-- 再次提醒：本脚本不含、也无法恢复你丢失的真实业务数据。
-- =====================================================================
