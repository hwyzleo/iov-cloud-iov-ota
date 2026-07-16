-- 添加 state_before_pause 字段到 tb_task 表
-- 用于记录暂停前的状态，恢复时直接回到该状态

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'state_before_pause');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `state_before_pause` SMALLINT DEFAULT NULL COMMENT ''暂停前的状态：6=RELEASED, 11=IN_PROGRESS'' AFTER `state`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
