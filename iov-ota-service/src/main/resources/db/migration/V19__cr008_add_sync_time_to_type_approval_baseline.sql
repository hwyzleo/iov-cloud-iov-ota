-- DSN-CR-008: 为tb_type_approval_baseline添加sync_time字段
-- 修复Bootstrap同步时间记录

ALTER TABLE `tb_type_approval_baseline` ADD COLUMN `sync_time` DATETIME DEFAULT NULL COMMENT '最后同步时间' AFTER `up_version`;
