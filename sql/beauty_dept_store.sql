-- =====================================================
-- 悦美部门归属门店迁移
-- 数据库：PostgreSQL
-- 说明：可重复执行。为历史 sys_dept 数据补 store_id，并确保每个有部门的租户至少有一个门店。
-- =====================================================

ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS store_id BIGINT;

COMMENT ON COLUMN sys_dept.store_id IS '所属门店ID';

WITH dept_tenants AS (
    SELECT DISTINCT tenant_id
    FROM sys_dept
    WHERE deleted = 0
      AND tenant_id IS NOT NULL
),
missing_store_tenants AS (
    SELECT dt.tenant_id
    FROM dept_tenants dt
    WHERE NOT EXISTS (
        SELECT 1
        FROM beauty_store s
        WHERE s.tenant_id = dt.tenant_id
          AND s.deleted = 0
    )
)
INSERT INTO beauty_store (
    tenant_id,
    store_code,
    store_name,
    status,
    remark,
    create_time,
    update_time,
    deleted
)
SELECT tenant_id,
       ('DEFAULT_' || tenant_id)::VARCHAR(32),
       '默认门店',
       1,
       '部门归属门店迁移自动创建',
       NOW(),
       NOW(),
       0
FROM missing_store_tenants
ON CONFLICT (tenant_id, store_code) DO NOTHING;

WITH first_store AS (
    SELECT DISTINCT ON (tenant_id)
           tenant_id,
           id
    FROM beauty_store
    WHERE deleted = 0
    ORDER BY tenant_id,
             CASE WHEN store_code LIKE 'DEFAULT_%' THEN 0 ELSE 1 END,
             id
)
UPDATE sys_dept d
SET store_id = fs.id,
    update_time = NOW()
FROM first_store fs
WHERE d.tenant_id = fs.tenant_id
  AND d.deleted = 0
  AND d.store_id IS NULL;

UPDATE sys_employee e
SET store_id = d.store_id,
    update_time = NOW()
FROM sys_dept d
WHERE e.dept_id = d.id
  AND e.deleted = 0
  AND d.deleted = 0
  AND e.store_id IS NULL
  AND d.store_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant_store_status ON sys_dept (tenant_id, store_id, status);
