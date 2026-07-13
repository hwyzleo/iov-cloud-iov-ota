-- 移除 tb_activity.version 字段（已被 activity_code 替代）
ALTER TABLE `tb_activity` DROP COLUMN `version`;
