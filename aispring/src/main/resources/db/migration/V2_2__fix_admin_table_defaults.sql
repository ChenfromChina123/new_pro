-- 为 admins 表的 is_active 字段添加默认值，解决 SQL Error 1364
ALTER TABLE admins MODIFY COLUMN is_active BIT(1) NOT NULL DEFAULT 1;
ALTER TABLE admins MODIFY COLUMN is_superadmin BIT(1) NOT NULL DEFAULT 0;
