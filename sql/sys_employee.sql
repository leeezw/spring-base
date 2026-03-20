-- =====================================================
-- 员工档案模块 DDL
-- 数据库：PostgreSQL
-- =====================================================

-- 员工表
CREATE TABLE IF NOT EXISTS sys_employee
(
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,

    -- 关联
    user_id             BIGINT,                              -- 关联系统账号（可空）
    dept_id             BIGINT,                              -- 所属部门
    post_id             BIGINT,                              -- 主岗位（可空）

    -- 基础档案
    emp_code            VARCHAR(32)  NOT NULL,               -- 工号
    emp_name            VARCHAR(64)  NOT NULL,               -- 员工姓名
    gender              SMALLINT     DEFAULT 0,              -- 性别：0未知 1男 2女
    phone               VARCHAR(20),
    email               VARCHAR(128),
    avatar              VARCHAR(512),

    -- 身份信息
    id_card             VARCHAR(18),                         -- 身份证号
    birthday            DATE,
    nation              VARCHAR(32),                         -- 民族
    native_place        VARCHAR(128),                        -- 籍贯
    address             VARCHAR(256),                        -- 现住址

    -- 入职信息
    hire_date           DATE,                                -- 入职日期
    emp_type            SMALLINT     DEFAULT 1,              -- 1正式 2试用 3实习
    probation_end_date  DATE,                                -- 试用期结束日期
    status              SMALLINT     NOT NULL DEFAULT 1,     -- 1在职 2试用中 0离职

    -- 紧急联系人
    emergency_contact   VARCHAR(64),
    emergency_phone     VARCHAR(20),
    emergency_relation  VARCHAR(32),

    -- 自定义字段（JSONB）
    custom_fields       JSONB,

    -- 审计字段
    create_time         TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    deleted             SMALLINT     NOT NULL DEFAULT 0,

    -- 同租户内工号唯一
    CONSTRAINT uk_employee_emp_code UNIQUE (emp_code, tenant_id)
);

COMMENT ON TABLE sys_employee IS '员工档案表';
COMMENT ON COLUMN sys_employee.user_id IS '关联系统账号ID（可空，未开通时为null）';
COMMENT ON COLUMN sys_employee.dept_id IS '所属部门ID';
COMMENT ON COLUMN sys_employee.post_id IS '主岗位ID（可空）';
COMMENT ON COLUMN sys_employee.emp_code IS '工号（同租户内唯一）';
COMMENT ON COLUMN sys_employee.gender IS '性别：0未知 1男 2女';
COMMENT ON COLUMN sys_employee.emp_type IS '员工类型：1正式 2试用 3实习';
COMMENT ON COLUMN sys_employee.status IS '在职状态：1在职 2试用中 0离职';
COMMENT ON COLUMN sys_employee.custom_fields IS '自定义字段值，key为SysEmployeeField.fieldKey';

-- custom_fields 索引（支持 @> 包含查询）
CREATE INDEX IF NOT EXISTS idx_employee_custom_fields ON sys_employee USING GIN (custom_fields);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_dept ON sys_employee (tenant_id, dept_id);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_status ON sys_employee (tenant_id, status);


-- =====================================================
-- 员工自定义字段定义表
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_employee_field
(
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,

    field_key   VARCHAR(64) NOT NULL,   -- 字段key，存入 custom_fields 时作为键
    field_label VARCHAR(64) NOT NULL,   -- 显示标签
    field_type  VARCHAR(16) NOT NULL,   -- text / number / date / select
    options     JSONB,                  -- 下拉选项，仅 field_type=select 时有效，示例：["A","B","AB","O"]
    required    SMALLINT    DEFAULT 0,  -- 1必填 0选填
    sort_order  INTEGER     DEFAULT 0,

    -- 审计字段
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT    NOT NULL DEFAULT 0,

    -- 同租户内 fieldKey 唯一
    CONSTRAINT uk_employee_field_key UNIQUE (field_key, tenant_id)
);

COMMENT ON TABLE sys_employee_field IS '员工自定义字段定义表';
COMMENT ON COLUMN sys_employee_field.field_key IS '字段key，对应 custom_fields JSONB 中的键';
COMMENT ON COLUMN sys_employee_field.field_type IS '字段类型：text/number/date/select';
COMMENT ON COLUMN sys_employee_field.options IS '下拉选项列表（JSON数组），仅 select 类型有效';
