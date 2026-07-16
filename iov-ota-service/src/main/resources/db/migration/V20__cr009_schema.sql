-- CR-009: 阶段模型 / 门禁 / 目标定义 / 条件建模 / 审批 / 状态机重排 / 断点续传重试
-- 设计文档：US-054 ~ US-063

-- 1. 修改 tb_task 表：状态机重排 + 新增字段
-- 步骤1.1：添加新字段（如果不存在）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'pause_reason');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `pause_reason` VARCHAR(50) DEFAULT NULL COMMENT ''暂停原因：MANUAL/GATE_HALT/RISK_HALT/COMPLIANCE_HALT'' AFTER `state`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'paused_by');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `paused_by` VARCHAR(50) DEFAULT NULL COMMENT ''暂停发起方：HUMAN/SYSTEM'' AFTER `pause_reason`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'auto_resume');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `auto_resume` TINYINT DEFAULT NULL COMMENT ''是否自动恢复（门禁暂停后自动恢复）'' AFTER `paused_by`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task' AND COLUMN_NAME = 'cancel_reason');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task` ADD COLUMN `cancel_reason` VARCHAR(50) DEFAULT NULL COMMENT ''取消原因：DISCARD/ABORT/ROLLBACK/COMPLIANCE/SUPERSEDED_BY'' AFTER `auto_resume`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤1.2：迁移现有状态值到新的四段式状态枚举
-- 兼容映射：PENDING→DRAFT, SUBMITTED→PENDING_APPROVAL, APPROVED→APPROVED, REJECTED→REJECTED, 
-- RELEASED→RELEASED, PAUSED→PAUSED, FINISHED→COMPLETED, CANCELLED→CANCELED
UPDATE `tb_task` SET `state` = CASE
    WHEN `state` = 1 THEN 1   -- PENDING -> DRAFT
    WHEN `state` = 2 THEN 2   -- SUBMITTED -> PENDING_APPROVAL
    WHEN `state` = 3 THEN 3   -- APPROVED -> APPROVED
    WHEN `state` = 4 THEN 4   -- REJECTED -> REJECTED
    WHEN `state` = 5 THEN 6   -- RELEASED -> RELEASED (新值6)
    WHEN `state` = 6 THEN 7   -- PAUSED -> PAUSED (新值7)
    WHEN `state` = 7 THEN 8   -- FINISHED -> COMPLETED (新值8)
    WHEN `state` = 8 THEN 9   -- CANCELLED -> CANCELED (新值9)
    ELSE `state`
END;

-- 步骤1.3：修改 state 字段注释
ALTER TABLE `tb_task`
    MODIFY COLUMN `state` SMALLINT NOT NULL COMMENT '任务状态：1=DRAFT, 2=PENDING_APPROVAL, 3=APPROVED, 4=REJECTED, 5=SCHEDULED, 6=RELEASED, 7=PAUSED, 8=COMPLETED, 9=CANCELED, 10=SUPERSEDED';

-- 步骤1.4：修改 phase 字段注释（固定不可变）
ALTER TABLE `tb_task`
    MODIFY COLUMN `phase` SMALLINT NOT NULL DEFAULT 1 COMMENT '任务阶段（固定不可变）：1=VALIDATION, 2=CANARY, 3=RELEASE';

-- 步骤1.5：修改 target 字段支持结构化目标定义
ALTER TABLE `tb_task`
    MODIFY COLUMN `target` TEXT NOT NULL COMMENT '升级目标（结构化JSON：mode=CONDITION/LIST/IMPORT，条件表达式或VIN列表）';

-- 步骤1.6：废弃 type 字段（保留但标记废弃）
-- 设计文档要求废弃 TaskType.LIGHT，但保留字段兼容性
ALTER TABLE `tb_task`
    MODIFY COLUMN `type` SMALLINT NOT NULL DEFAULT 1 COMMENT '任务类型（废弃LIGHT，仅保留NORMAL=1）';

-- 2. 新增 tb_task_phase_gate 表（跨任务阶段门禁）
CREATE TABLE IF NOT EXISTS `tb_task_phase_gate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '升级活动ID',
    `from_phase` SMALLINT NOT NULL COMMENT '来源阶段：1=VALIDATION, 2=CANARY, 3=RELEASE',
    `to_phase` SMALLINT NOT NULL COMMENT '目标阶段：1=VALIDATION, 2=CANARY, 3=RELEASE',
    `prev_task_id` BIGINT DEFAULT NULL COMMENT '前序任务ID',
    `gate_state` VARCHAR(20) NOT NULL COMMENT '门禁状态：PASS/FAIL/PENDING',
    `override` TINYINT DEFAULT 0 COMMENT '是否人工跳阶授权',
    `approval_ref` VARCHAR(255) DEFAULT NULL COMMENT '审批引用（跳阶授权时）',
    `decided_by` VARCHAR(64) DEFAULT NULL COMMENT '决策人',
    `decided_at` DATETIME DEFAULT NULL COMMENT '决策时间',
    `report_ref` VARCHAR(255) DEFAULT NULL COMMENT '报告引用',
    `gate_threshold_snapshot` TEXT DEFAULT NULL COMMENT '门禁阈值快照（JSON）',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_activity_phase` (`activity_id`, `to_phase`),
    INDEX `idx_activity` (`activity_id`),
    INDEX `idx_prev_task` (`prev_task_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '跨任务阶段门禁表';

-- 3. 新增 tb_phase_gate_policy 表（阶段门禁阈值策略）
CREATE TABLE IF NOT EXISTS `tb_phase_gate_policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phase` SMALLINT NOT NULL COMMENT '阶段：1=VALIDATION, 2=CANARY, 3=RELEASE',
    `activity_id` BIGINT DEFAULT NULL COMMENT '活动级覆盖（可空，空为全局策略）',
    `success_rate_min` DECIMAL(5,4) DEFAULT NULL COMMENT '成功率最小阈值',
    `fail_cnt_max` INT DEFAULT NULL COMMENT '失败数最大阈值',
    `severe_defect_allowed` TINYINT DEFAULT 0 COMMENT '是否允许严重缺陷',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_phase_activity` (`phase`, `activity_id`),
    INDEX `idx_phase` (`phase`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '阶段门禁阈值策略表';

-- 4. 新增 tb_install_condition_type 表（安装条件类型受控词表）
CREATE TABLE IF NOT EXISTS `tb_install_condition_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(50) NOT NULL COMMENT '条件编码：KEEP_IN_PARK/NOT_CHARGING/NO_EXTERNAL_POWER/ALL_CLOSED/HV_SOC/LV_SOC/AMBIENT_TEMP/POWER_MODE/NETWORK_TYPE/SIGNAL_STRENGTH',
    `name` VARCHAR(100) NOT NULL COMMENT '条件名称',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `value_type` VARCHAR(20) NOT NULL COMMENT '值类型：BOOLEAN/INTEGER/DECIMAL/STRING',
    `default_value` VARCHAR(255) DEFAULT NULL COMMENT '默认值',
    `applicable_phase` VARCHAR(50) DEFAULT NULL COMMENT '适用阶段（逗号分隔）：VALIDATION,CANARY,RELEASE',
    `mandatory` TINYINT DEFAULT 0 COMMENT '是否必选',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '安装条件类型受控词表';

-- 5. 新增 tb_task_install_condition 表（任务实例化安装条件）
CREATE TABLE IF NOT EXISTS `tb_task_install_condition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `condition_type` VARCHAR(50) NOT NULL COMMENT '条件类型编码',
    `operator` VARCHAR(20) NOT NULL COMMENT '操作符：EQ/NE/GT/GE/LT/LE/IN/NOT_IN',
    `threshold` VARCHAR(255) NOT NULL COMMENT '阈值',
    `severity` VARCHAR(20) NOT NULL DEFAULT 'BLOCK' COMMENT '严重级别：BLOCK/WARN',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task` (`task_id`),
    INDEX `idx_condition_type` (`condition_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务实例化安装条件表';

-- 6. 新增 tb_task_approval 表（任务审批链留痕）
CREATE TABLE IF NOT EXISTS `tb_task_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `level` VARCHAR(20) NOT NULL COMMENT '审批级别：QUALITY/PRODUCT/SECURITY',
    `approver` VARCHAR(64) NOT NULL COMMENT '审批人',
    `result` VARCHAR(20) NOT NULL COMMENT '审批结果：APPROVED/REJECTED',
    `comment` TEXT DEFAULT NULL COMMENT '审批意见',
    `decided_at` DATETIME NOT NULL COMMENT '审批时间',
    `approval_ref` VARCHAR(255) DEFAULT NULL COMMENT '审批引用（跳阶授权时）',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task` (`task_id`),
    INDEX `idx_level` (`level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务审批链留痕表';

-- 7. 新增 tb_phase_approval_policy 表（按阶段审批要求）
CREATE TABLE IF NOT EXISTS `tb_phase_approval_policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phase` SMALLINT NOT NULL COMMENT '阶段：1=VALIDATION, 2=CANARY, 3=RELEASE',
    `activity_id` BIGINT DEFAULT NULL COMMENT '活动级覆盖（可空，空为全局策略）',
    `required` TINYINT NOT NULL DEFAULT 1 COMMENT '是否需要审批',
    `required_levels` VARCHAR(100) NOT NULL COMMENT '需要审批级别（逗号分隔）：QUALITY,PRODUCT,SECURITY',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_phase_activity` (`phase`, `activity_id`),
    INDEX `idx_phase` (`phase`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '按阶段审批要求表';

-- 8. 新增 tb_task_state_log 表（状态迁移审计）
CREATE TABLE IF NOT EXISTS `tb_task_state_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `from_state` SMALLINT NOT NULL COMMENT '原状态',
    `to_state` SMALLINT NOT NULL COMMENT '新状态',
    `action` VARCHAR(50) NOT NULL COMMENT '操作：SUBMIT/AUDIT/RELEASE/PAUSE/RESUME/CANCEL/FINISH/SUPERSEDE',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `reason` TEXT DEFAULT NULL COMMENT '原因',
    `decided_at` DATETIME NOT NULL COMMENT '决策时间',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task` (`task_id`),
    INDEX `idx_action` (`action`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务状态迁移审计表';

-- 9. 修改 tb_task_batch 表：删除 phase 列（兼容清理）
-- 设计文档要求删除 phase 列，因为 phase 已移至 tb_task 主表
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_batch' AND COLUMN_NAME = 'phase');
SET @sql = IF(@column_exists > 0, 
    'ALTER TABLE `tb_task_batch` DROP COLUMN `phase`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 10. 修改 tb_task_vehicle 表：增列（目标解析后车辆快照 + 重试/续传）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'source');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `source` VARCHAR(20) DEFAULT NULL COMMENT ''目标来源：CONDITION/LIST/IMPORT'' AFTER `task_id`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'admit_state');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `admit_state` VARCHAR(20) DEFAULT NULL COMMENT ''准入状态：PASS/REJECT'' AFTER `source`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'admit_reason');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `admit_reason` TEXT DEFAULT NULL COMMENT ''准入原因（REJECT时）'' AFTER `admit_state`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'baseline');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `baseline` VARCHAR(255) DEFAULT NULL COMMENT ''基线快照'' AFTER `admit_reason`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'download_retry_count');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `download_retry_count` INT DEFAULT 0 COMMENT ''下载重试次数'' AFTER `baseline`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'install_retry_count');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `install_retry_count` INT DEFAULT 0 COMMENT ''安装重试次数'' AFTER `download_retry_count`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'resume_offset');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `resume_offset` BIGINT DEFAULT NULL COMMENT ''续传偏移量（字节）'' AFTER `install_retry_count`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'resume_token');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `resume_token` VARCHAR(255) DEFAULT NULL COMMENT ''续传令牌'' AFTER `resume_offset`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'last_fail_reason');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `last_fail_reason` TEXT DEFAULT NULL COMMENT ''最近失败原因'' AFTER `resume_token`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'next_retry_at');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `next_retry_at` DATETIME DEFAULT NULL COMMENT ''下次重试时间'' AFTER `last_fail_reason`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_vehicle' AND COLUMN_NAME = 'attempt_no');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_vehicle` ADD COLUMN `attempt_no` INT DEFAULT 0 COMMENT ''尝试次数（幂等）'' AFTER `next_retry_at`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 11. 修改 tb_task_restriction 表：补强（依赖/兼容维度）
-- 设计文档要求补依赖/兼容维度，这里添加新字段支持更复杂的限制条件
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_restriction' AND COLUMN_NAME = 'restriction_level');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_restriction` ADD COLUMN `restriction_level` VARCHAR(20) DEFAULT NULL COMMENT ''限制级别：ERROR/WARNING/INFO'' AFTER `restriction_expression`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_restriction' AND COLUMN_NAME = 'error_message');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_restriction` ADD COLUMN `error_message` TEXT DEFAULT NULL COMMENT ''错误信息模板'' AFTER `restriction_level`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 12. 修改 tb_task_strategy 表：增补（下载重试策略）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_strategy' AND COLUMN_NAME = 'download_retry_max');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_strategy` ADD COLUMN `download_retry_max` INT DEFAULT NULL COMMENT ''下载重试最大次数（独立于FLASH_COUNT）'' AFTER `strategy_expression`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_strategy' AND COLUMN_NAME = 'retry_backoff');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_strategy` ADD COLUMN `retry_backoff` VARCHAR(20) DEFAULT NULL COMMENT ''重试退避策略：FIXED/EXP'' AFTER `download_retry_max`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_task_strategy' AND COLUMN_NAME = 'resume_on_poweroff');
SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE `tb_task_strategy` ADD COLUMN `resume_on_poweroff` TINYINT DEFAULT 0 COMMENT ''断电后是否续传'' AFTER `retry_backoff`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 13. 新增 tb_task_vehicle_retry_log 表（重试/续传轨迹审计）
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_retry_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `vin` VARCHAR(17) NOT NULL COMMENT 'VIN',
    `stage` VARCHAR(20) NOT NULL COMMENT '阶段：DOWNLOAD/INSTALL',
    `attempt_no` INT NOT NULL COMMENT '尝试次数',
    `offset` BIGINT DEFAULT NULL COMMENT '偏移量（字节）',
    `result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAIL',
    `reason` TEXT DEFAULT NULL COMMENT '原因',
    `retried_at` DATETIME NOT NULL COMMENT '重试时间',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_vin` (`task_id`, `vin`),
    INDEX `idx_stage` (`stage`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '重试/续传轨迹审计表';

-- 14. 插入默认的安装条件类型数据（如果不存在）
INSERT IGNORE INTO `tb_install_condition_type` (`code`, `name`, `unit`, `value_type`, `default_value`, `applicable_phase`, `mandatory`) VALUES
('KEEP_IN_PARK', '驻车状态', NULL, 'BOOLEAN', 'true', 'VALIDATION,CANARY,RELEASE', 1),
('NOT_CHARGING', '非充电状态', NULL, 'BOOLEAN', 'true', 'VALIDATION,CANARY,RELEASE', 1),
('NO_EXTERNAL_POWER', '无外部电源', NULL, 'BOOLEAN', 'true', 'VALIDATION,CANARY,RELEASE', 1),
('ALL_CLOSED', '车门关闭', NULL, 'BOOLEAN', 'true', 'VALIDATION,CANARY,RELEASE', 1),
('HV_SOC', '高压电量', '%', 'INTEGER', '30', 'CANARY,RELEASE', 0),
('LV_SOC', '低压电量', '%', 'INTEGER', '50', 'CANARY,RELEASE', 0),
('AMBIENT_TEMP', '环境温度', '°C', 'DECIMAL', '0', 'CANARY,RELEASE', 0),
('POWER_MODE', '电源模式', NULL, 'STRING', 'OFF', 'CANARY,RELEASE', 0),
('NETWORK_TYPE', '网络类型', NULL, 'STRING', 'WIFI', 'CANARY,RELEASE', 0),
('SIGNAL_STRENGTH', '信号强度', 'dBm', 'INTEGER', '-80', 'CANARY,RELEASE', 0);

-- 15. 插入默认的阶段门禁策略数据（如果不存在）
INSERT IGNORE INTO `tb_phase_gate_policy` (`phase`, `success_rate_min`, `fail_cnt_max`, `severe_defect_allowed`) VALUES
(1, 0.95, 10, 0),  -- VALIDATION阶段：成功率≥95%，失败数≤10，不允许严重缺陷
(2, 0.90, 50, 0),  -- CANARY阶段：成功率≥90%，失败数≤50，不允许严重缺陷
(3, 0.85, 100, 0); -- RELEASE阶段：成功率≥85%，失败数≤100，不允许严重缺陷

-- 16. 插入默认的阶段审批策略数据（如果不存在）
INSERT IGNORE INTO `tb_phase_approval_policy` (`phase`, `required`, `required_levels`) VALUES
(1, 0, ''),  -- VALIDATION阶段：默认不需要审批
(2, 1, 'QUALITY,PRODUCT'),  -- CANARY阶段：需要质量、产品审批
(3, 1, 'QUALITY,PRODUCT,SECURITY'); -- RELEASE阶段：需要全部三级审批
