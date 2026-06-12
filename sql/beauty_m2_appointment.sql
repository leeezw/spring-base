-- =====================================================
-- 悦美 M2：会员详情与预约主流程
-- 数据库：PostgreSQL
-- =====================================================

CREATE TABLE IF NOT EXISTS beauty_appointment
(
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

COMMENT ON TABLE beauty_appointment IS '美业预约表';
COMMENT ON COLUMN beauty_appointment.status IS '预约状态：0待确认 1已确认 2已到店 3服务中 4已完成 5已取消 6爽约';
COMMENT ON COLUMN beauty_appointment.service_item_id IS '服务项目ID，M3项目主数据上线后使用';
COMMENT ON COLUMN beauty_appointment.service_item_name IS '预约项目名称';

CREATE INDEX IF NOT EXISTS idx_beauty_appointment_store_time ON beauty_appointment (tenant_id, store_id, start_time, status);
CREATE INDEX IF NOT EXISTS idx_beauty_appointment_member ON beauty_appointment (tenant_id, member_id, start_time);
CREATE INDEX IF NOT EXISTS idx_beauty_appointment_employee_time ON beauty_appointment (tenant_id, employee_id, start_time, end_time);

CREATE TABLE IF NOT EXISTS beauty_appointment_log
(
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

COMMENT ON TABLE beauty_appointment_log IS '美业预约操作日志';

CREATE INDEX IF NOT EXISTS idx_beauty_appointment_log_appointment ON beauty_appointment_log (tenant_id, appointment_id, create_time);

CREATE TABLE IF NOT EXISTS beauty_member_follow_record
(
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

COMMENT ON TABLE beauty_member_follow_record IS '美业会员跟进记录';

CREATE INDEX IF NOT EXISTS idx_beauty_member_follow_member ON beauty_member_follow_record (tenant_id, member_id, create_time);
CREATE INDEX IF NOT EXISTS idx_beauty_member_follow_next ON beauty_member_follow_record (tenant_id, next_follow_time);

INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, icon, sort_order, visible, status, create_time, update_time, deleted)
SELECT 0, parent.id, '预约中心', '/beauty/appointment', 'beauty/appointment/index', 'tabler-calendar-time', 40, 1, 1, NOW(), NOW(), 0
FROM sys_menu parent
WHERE parent.tenant_id = 0
  AND parent.menu_name = '悦美经营'
  AND parent.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE tenant_id = 0 AND path = '/beauty/appointment' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:appointment:query', '预约查询', 2, 0, 170, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:appointment:query' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:appointment:add', '预约新增', 2, 0, 180, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:appointment:add' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:appointment:edit', '预约编辑', 2, 0, 190, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:appointment:edit' AND deleted = 0);

INSERT INTO sys_permission (tenant_id, permission_code, permission_name, permission_type, parent_id, sort_order, status, create_time, update_time, deleted)
SELECT 0, 'beauty:appointment:delete', '预约删除', 2, 0, 200, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'beauty:appointment:delete' AND deleted = 0);

INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.id, m.id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.deleted = 0 AND m.path = '/beauty/appointment'
WHERE r.deleted = 0
  AND (r.role_code IN ('ADMIN', 'ROLE_ADMIN') OR r.role_code = '*')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.deleted = 0 AND p.permission_code LIKE 'beauty:appointment:%'
WHERE r.deleted = 0
  AND (r.role_code IN ('ADMIN', 'ROLE_ADMIN') OR r.role_code = '*')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
