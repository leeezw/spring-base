-- 职位表
CREATE TABLE sys_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL COMMENT '所属岗位ID',
    position_code VARCHAR(50) NOT NULL COMMENT '职位编码',
    position_name VARCHAR(100) NOT NULL COMMENT '职位名称',
    position_level INT COMMENT '职级：1组长 2主管 3经理 4总监',
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1 COMMENT '状态：1启用 0停用',
    description VARCHAR(500),
    create_time DATETIME,
    update_time DATETIME,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT DEFAULT 0,
    tenant_id BIGINT,
    INDEX idx_post_id (post_id),
    INDEX idx_tenant (tenant_id)
) COMMENT='职位表';

-- 如果表已存在，删除唯一约束
ALTER TABLE sys_position DROP INDEX IF EXISTS uk_code_tenant;

-- 员工表增加职位字段
ALTER TABLE sys_employee ADD COLUMN IF NOT EXISTS position_id BIGINT COMMENT '职位ID' AFTER post_id;
