-- CR-015 P0: 多任务放量门禁与任务报告
-- 设计增量见 IOV-OTA-DSN-CR-015 §3.2 / §9-P0。
-- 原则：多任务波次模型（sequence_no + previous_task_id），基于前序正式报告的门禁；
--       不实现 TaskBatch / 批次车辆映射；tb_task_phase_gate 泛化迁移为 tb_task_release_gate。

-- ============================================================
-- 1. tb_task：新增波次序与前一任务引用
--    UK(activity_id, sequence_no) 保证同活动内波次唯一；
--    previous_task_id 表达前序任务（用于放行门禁），软引用不建外键。
-- ============================================================
ALTER TABLE `tb_task`
    ADD COLUMN `sequence_no` INT NOT NULL DEFAULT 0 COMMENT '活动内放量波次序（同一Activity下唯一）' AFTER `activity_id`,
    ADD COLUMN `previous_task_id` BIGINT DEFAULT NULL COMMENT '前序任务ID（用于多任务放行门禁，软引用）' AFTER `sequence_no`;

ALTER TABLE `tb_task`
    ADD UNIQUE KEY `uk_activity_sequence` (`activity_id`, `sequence_no`),
    ADD KEY `idx_task_prev` (`previous_task_id`);

-- ============================================================
-- 2. 新建 tb_task_release_gate（由 tb_task_phase_gate 泛化迁移）
--    门禁挂在下一任务（next_task_id）上；UK(next_task_id)；
--    gate_type: SAME_PHASE(同阶段波次) / CROSS_PHASE(跨阶段推进)
--    gate_state: PASS / FAIL / PENDING
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_release_gate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '升级活动ID',
    `previous_task_id` BIGINT DEFAULT NULL COMMENT '前序任务ID',
    `next_task_id` BIGINT NOT NULL COMMENT '下一任务ID（波次/下一阶段）',
    `gate_type` VARCHAR(20) NOT NULL DEFAULT 'SAME_PHASE' COMMENT '门禁类型：SAME_PHASE/CROSS_PHASE',
    `gate_state` VARCHAR(20) NOT NULL COMMENT '门禁状态：PASS/FAIL/PENDING',
    `gate_threshold_snapshot` TEXT DEFAULT NULL COMMENT '门禁阈值快照（JSON）',
    `report_ref` VARCHAR(255) DEFAULT NULL COMMENT '前序正式报告引用（reportVersion）',
    `override` TINYINT DEFAULT 0 COMMENT '是否人工放行（override）',
    `approval_ref` VARCHAR(255) DEFAULT NULL COMMENT '审批引用（override时）',
    `decided_by` VARCHAR(64) DEFAULT NULL COMMENT '决策人',
    `decided_at` DATETIME DEFAULT NULL COMMENT '决策时间',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `row_version` INT DEFAULT 1 COMMENT '记录版本',
    `row_valid` TINYINT DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_release_gate_next` (`next_task_id`),
    KEY `idx_release_gate_prev_state` (`previous_task_id`, `gate_state`),
    KEY `idx_release_gate_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '多任务放行门禁表';

-- 旧跨任务阶段门禁表已泛化迁移，直接删除（无存量数据，见 CR-015 §9-P2-B）
DROP TABLE IF EXISTS `tb_task_phase_gate`;

-- ============================================================
-- 3. tb_task_metric：改为 Task 级统计，去除 batchNo 业务维度
-- ============================================================
ALTER TABLE `tb_task_metric`
    DROP INDEX `idx_task_batch`,
    DROP COLUMN `batch_no`;

ALTER TABLE `tb_task_metric`
    ADD KEY `idx_task_metric_stat` (`task_id`, `stat_time`);

-- ============================================================
-- 4. tb_task_report：增加 report_version 幂等（正式报告写后不可原地覆盖）
-- ============================================================
ALTER TABLE `tb_task_report`
    ADD COLUMN `report_version` INT NOT NULL DEFAULT 1 COMMENT '正式报告版本（幂等，UK(task_id,report_version)）' AFTER `task_id`;

ALTER TABLE `tb_task_report`
    ADD UNIQUE KEY `uk_task_report_version` (`task_id`, `report_version`);
