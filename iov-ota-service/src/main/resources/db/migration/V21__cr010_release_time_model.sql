-- CR-010: 发布与执行时间模型
-- 设计文档：US-064 ~ US-067

-- 1. 修改 tb_task 表：新增发布时间相关字段
-- 步骤1.1：添加 actual_release_time 字段
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'actual_release_time');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `actual_release_time` DATETIME DEFAULT NULL COMMENT ''实际发布时间（发布事务成功时间）'' AFTER `release_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤1.2：添加 last_schedule_error 字段
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'last_schedule_error');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `last_schedule_error` VARCHAR(500) DEFAULT NULL COMMENT ''最近一次到点发布失败摘要'' AFTER `actual_release_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 修改 state 字段注释，新增 IN_PROGRESS 状态
-- IN_PROGRESS = 11（在SUPERSEDED=10之后）
ALTER TABLE `tb_task`
    MODIFY COLUMN `state` SMALLINT NOT NULL COMMENT '任务状态：1=DRAFT, 2=PENDING_APPROVAL, 3=APPROVED, 4=REJECTED, 5=SCHEDULED, 6=RELEASED, 7=PAUSED, 8=COMPLETED, 9=CANCELED, 10=SUPERSEDED, 11=IN_PROGRESS';

-- 3. 新增索引：用于到期任务扫描
-- IDX_TASK_SCHEDULE_DUE(state, release_time)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND INDEX_NAME = 'IDX_TASK_SCHEDULE_DUE');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX `IDX_TASK_SCHEDULE_DUE` ON `tb_task` (`state`, `release_time`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 修改 tb_task_state_log 表：新增 SCHEDULE/UNSCHEDULE/RELEASE/ACTIVATE 操作类型
-- 这些操作类型在代码中定义，数据库层面仅需确保 action 字段长度足够
ALTER TABLE `tb_task_state_log`
    MODIFY COLUMN `action` VARCHAR(50) NOT NULL COMMENT '操作：SUBMIT/AUDIT/RELEASE/PAUSE/RESUME/CANCEL/FINISH/SUPERSEDE/SCHEDULE/UNSCHEDULE/ACTIVATE';
