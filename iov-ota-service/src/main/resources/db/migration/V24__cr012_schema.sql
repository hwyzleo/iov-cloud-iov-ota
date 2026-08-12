-- CR-012: Task/VehicleTask/Execution 三层运行模型
-- 建立 VehicleTask/Execution 持久化、清单快照、授权凭据、包阶段结果、
-- 安装许可、顺序事件水位、控制回执、最终收口、日志、恢复、幂等与事务性 Outbox。
-- 表名按 tb_task_vehicle_execution* 族对齐现有约定（设计稿 §6 允许保持业务契约不变地调整命名）。

-- ============================================================
-- 1. tb_vehicle_inventory: 同 VIN 当前已接受的完整 ECU 清单头
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_inventory` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                 VARCHAR(20)  NOT NULL COMMENT '车架号',
    `inventory_revision`  BIGINT       NOT NULL COMMENT '清单版本号',
    `digest`              VARCHAR(128) NOT NULL COMMENT '清单摘要',
    `algorithm`           VARCHAR(32)  NOT NULL DEFAULT 'SHA-256' COMMENT '摘要算法',
    `accepted_time`       DATETIME(3)  NOT NULL COMMENT '清单接受时间',
    `create_time`         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`           BIGINT       NULL COMMENT '创建人',
    `modify_time`         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`           BIGINT       NULL COMMENT '修改人',
    `row_version`         BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`           TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_inventory_vin_rev` (`vin`, `inventory_revision`),
    KEY `idx_vehicle_inventory_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆ECU清单头（CR-012）';

-- ============================================================
-- 2. tb_vehicle_inventory_item: ECU 清单明细
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_inventory_item` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inventory_id`    BIGINT       NOT NULL COMMENT '清单头ID',
    `ecu_id`          VARCHAR(64)  NOT NULL COMMENT 'ECU标识',
    `ecu_name`        VARCHAR(128) NULL COMMENT 'ECU名称',
    `software_pn`     VARCHAR(64)  NULL COMMENT '软件零件号',
    `software_version` VARCHAR(64) NULL COMMENT '软件版本',
    `hardware_pn`     VARCHAR(64)  NULL COMMENT '硬件零件号',
    `hardware_version` VARCHAR(64) NULL COMMENT '硬件版本',
    `create_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`       BIGINT       NULL COMMENT '创建人',
    `modify_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`       BIGINT       NULL COMMENT '修改人',
    `row_version`     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`       TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inventory_item_inv_ecu` (`inventory_id`, `ecu_id`),
    KEY `idx_inventory_item_inventory` (`inventory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆ECU清单明细（CR-012）';

-- ============================================================
-- 3. tb_vehicle_task_snapshot: VehicleTask 不可变快照
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_task_snapshot` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vehicle_task_id` BIGINT       NOT NULL COMMENT '车辆任务ID',
    `task_revision`   BIGINT       NOT NULL COMMENT '任务版本号',
    `snapshot_digest` VARCHAR(128) NOT NULL COMMENT '快照摘要',
    `snapshot_content` TEXT        NOT NULL COMMENT '快照内容（JSON）',
    `create_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`       BIGINT       NULL COMMENT '创建人',
    `modify_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`       BIGINT       NULL COMMENT '修改人',
    `row_version`     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`       TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vt_snapshot_vt_rev` (`vehicle_task_id`, `task_revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆任务快照（CR-012）';

-- ============================================================
-- 4. tb_vehicle_task_consent: 授权凭据
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_task_consent` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vehicle_task_id`      BIGINT       NOT NULL COMMENT '车辆任务ID',
    `consent_receipt_id`   VARCHAR(64)  NOT NULL COMMENT '授权回执ID（幂等键）',
    `terms_id`             BIGINT       NOT NULL COMMENT '条款文章ID',
    `terms_hash`           VARCHAR(128) NOT NULL COMMENT '条款摘要',
    `consent_scope_digest` VARCHAR(128) NOT NULL COMMENT '授权范围摘要',
    `consent_state`        VARCHAR(30)  NOT NULL COMMENT '授权状态：NOT_REQUIRED/PENDING/GRANTED/DENIED/REVOKED/EXPIRED',
    `accepted`             TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已接受',
    `effective_state`      VARCHAR(30)  NULL COMMENT '有效授权状态（与accepted分离）',
    `revoked_time`         DATETIME(3)  NULL COMMENT '撤回时间',
    `reconsent_required`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需重新授权',
    `create_time`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`            BIGINT       NULL COMMENT '创建人',
    `modify_time`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`            BIGINT       NULL COMMENT '修改人',
    `row_version`          BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`            TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vt_consent_receipt` (`consent_receipt_id`),
    KEY `idx_vt_consent_vt` (`vehicle_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆任务授权凭据（CR-012）';

-- ============================================================
-- 5. tb_vehicle_task_package: 车辆任务包快照
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_task_package` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vehicle_task_id`  BIGINT       NOT NULL COMMENT '车辆任务ID',
    `package_id`       VARCHAR(64)  NOT NULL COMMENT '包ID',
    `package_revision` VARCHAR(64)  NULL COMMENT '包版本号',
    `etag`             VARCHAR(128) NULL COMMENT '对象ETag',
    `download_state`   VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT '下载准备状态',
    `verify_state`     VARCHAR(30)  NULL COMMENT '校验状态',
    `create_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`        BIGINT       NULL COMMENT '创建人',
    `modify_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`        BIGINT       NULL COMMENT '修改人',
    `row_version`      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vt_package_vt_pkg` (`vehicle_task_id`, `package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆任务包快照（CR-012）';

-- ============================================================
-- 6. tb_package_stage_result: 下载/验签/解密终态
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_package_stage_result` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stage_result_id`  VARCHAR(64)  NOT NULL COMMENT '阶段结果ID（幂等键）',
    `vehicle_task_id`  BIGINT       NOT NULL COMMENT '车辆任务ID',
    `package_id`       VARCHAR(64)  NOT NULL COMMENT '包ID',
    `stage`            VARCHAR(20)  NOT NULL COMMENT '阶段：DOWNLOAD/VERIFY/DECRYPT',
    `result_status`    VARCHAR(20)  NOT NULL COMMENT '结果：SUCCESS/FAILED',
    `package_revision` VARCHAR(64)  NULL COMMENT '包版本号',
    `etag`             VARCHAR(128) NULL COMMENT '对象ETag',
    `digest`           VARCHAR(128) NULL COMMENT '包摘要',
    `signature_result` VARCHAR(20)  NULL COMMENT '签名校验结果',
    `decrypt_result`   VARCHAR(20)  NULL COMMENT '解密结果',
    `fail_reason`      VARCHAR(500) NULL COMMENT '失败原因',
    `create_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`        BIGINT       NULL COMMENT '创建人',
    `modify_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`        BIGINT       NULL COMMENT '修改人',
    `row_version`      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stage_result_id` (`stage_result_id`),
    KEY `idx_stage_result_vt` (`vehicle_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包阶段结果（CR-012）';

-- ============================================================
-- 7. tb_task_vehicle_execution: 一次安装尝试主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `execution_id`             VARCHAR(64)  NOT NULL COMMENT '执行ID（业务键）',
    `vehicle_task_id`          BIGINT       NOT NULL COMMENT '车辆任务ID',
    `attempt_no`               INT          NOT NULL COMMENT '尝试序号',
    `status`                   VARCHAR(30)  NOT NULL COMMENT '执行状态：PERMITTED/INSTALLING/PAUSED/ROLLING_BACK/SUCCEEDED/FAILED/ROLLED_BACK/CANCELED/TIMED_OUT',
    `task_revision`            BIGINT       NOT NULL COMMENT '冻结的任务版本',
    `install_plan_version`     VARCHAR(64)  NULL COMMENT '冻结的安装计划版本',
    `package_manifest_digest`  VARCHAR(128) NULL COMMENT '冻结的包清单摘要',
    `condition_set_version`    VARCHAR(64)  NULL COMMENT '冻结的条件集版本',
    `permit_token`             VARCHAR(512) NULL COMMENT '安装许可令牌',
    `offline_policy`           TEXT         NULL COMMENT '离线策略（JSON）',
    `timeout_policy`           TEXT         NULL COMMENT '超时策略（JSON）',
    `control_policy`           TEXT         NULL COMMENT '控制策略（JSON）',
    `valid_until`              DATETIME(3)  NULL COMMENT '许可有效期（仅限制进入INSTALL_STARTED）',
    `accepted_sequence_no`     BIGINT       NOT NULL DEFAULT 0 COMMENT '事件连续水位',
    `final_sequence_no`        BIGINT       NOT NULL DEFAULT 0 COMMENT '最终序号',
    `start_time`               DATETIME(3)  NULL COMMENT '安装开始时间',
    `end_time`                 DATETIME(3)  NULL COMMENT '收口时间',
    `create_time`              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`                BIGINT       NULL COMMENT '创建人',
    `modify_time`              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`                BIGINT       NULL COMMENT '修改人',
    `row_version`              BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`                TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_attempt` (`vehicle_task_id`, `attempt_no`),
    KEY `idx_execution_vt` (`vehicle_task_id`),
    KEY `idx_execution_id` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行主表（CR-012）';

-- ============================================================
-- 8. tb_task_vehicle_execution_active: 活动执行占位表
--    解决 MySQL 可空唯一键并发漏洞；UK(vehicle_task_id) 保证同一 VehicleTask 同时最多一个活动 Execution
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_active` (
    `vehicle_task_id` BIGINT NOT NULL COMMENT '车辆任务ID（主键）',
    `execution_id`    BIGINT NOT NULL COMMENT '活动执行ID',
    `create_time`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`vehicle_task_id`),
    KEY `idx_exec_active_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动执行占位表（CR-012 RD-012-5）';

-- ============================================================
-- 9. tb_task_vehicle_execution_event: 安装事件
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_event` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id`        VARCHAR(64)  NOT NULL COMMENT '事件ID（幂等键）',
    `execution_id`    BIGINT       NOT NULL COMMENT '执行ID',
    `sequence_no`     BIGINT       NOT NULL COMMENT '事件序号',
    `event_type`      VARCHAR(50)  NULL COMMENT '事件类型',
    `event_digest`    VARCHAR(128) NULL COMMENT '事件摘要（防冲突）',
    `event_payload`   TEXT         NULL COMMENT '事件负载（JSON）',
    `disposition`     VARCHAR(20)  NULL COMMENT '处置：ACCEPTED/DUPLICATE/BUFFERED/REJECTED/CONFLICT',
    `received_time`   DATETIME(3)  NOT NULL COMMENT '接收时间',
    `create_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`       BIGINT       NULL COMMENT '创建人',
    `modify_time`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`       BIGINT       NULL COMMENT '修改人',
    `row_version`     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`       TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exec_event_id` (`event_id`),
    UNIQUE KEY `uk_exec_event_seq` (`execution_id`, `sequence_no`),
    KEY `idx_exec_event_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行事件（CR-012）';

-- ============================================================
-- 10. tb_task_vehicle_execution_control: 云端控制指令
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_control` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `control_id`       VARCHAR(64)  NOT NULL COMMENT '控制ID（幂等键）',
    `execution_id`     BIGINT       NOT NULL COMMENT '执行ID',
    `control_revision` INT          NOT NULL COMMENT '控制版本号，单调递增',
    `action`           VARCHAR(20)  NOT NULL COMMENT '控制动作：NONE/CONTINUE/PAUSE/ABORT/ROLLBACK/RESYNC',
    `scope`            VARCHAR(50)  NULL COMMENT '控制作用域',
    `apply_mode`       VARCHAR(20)  NULL COMMENT '应用模式：IMMEDIATE/SAFE_POINT',
    `reason`           VARCHAR(500) NULL COMMENT '控制原因',
    `create_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`        BIGINT       NULL COMMENT '创建人',
    `modify_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`        BIGINT       NULL COMMENT '修改人',
    `row_version`      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_control_id` (`control_id`),
    UNIQUE KEY `uk_control_revision` (`execution_id`, `control_revision`),
    KEY `idx_control_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行控制指令（CR-012）';

-- ============================================================
-- 11. tb_task_vehicle_execution_control_ack: 控制回执
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_control_ack` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `control_ack_id`   VARCHAR(64)  NOT NULL COMMENT '控制回执ID（幂等键）',
    `control_id`       VARCHAR(64)  NOT NULL COMMENT '控制ID',
    `execution_id`     BIGINT       NOT NULL COMMENT '执行ID',
    `ack_sequence_no`  INT          NOT NULL COMMENT '回执序号',
    `ack_status`       VARCHAR(20)  NOT NULL COMMENT '回执状态：RECEIVED/DEFERRED/APPLIED/REJECTED',
    `ack_payload`      TEXT         NULL COMMENT '回执负载（JSON）',
    `ack_time`         DATETIME(3)  NOT NULL COMMENT '回执时间',
    `create_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`        BIGINT       NULL COMMENT '创建人',
    `modify_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`        BIGINT       NULL COMMENT '修改人',
    `row_version`      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_control_ack_id` (`control_ack_id`),
    UNIQUE KEY `uk_control_ack_seq` (`control_id`, `ack_sequence_no`),
    KEY `idx_control_ack_control` (`control_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行控制回执（CR-012）';

-- ============================================================
-- 12. tb_task_vehicle_execution_ecu_result: 收口后 ECU 实际结果
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_ecu_result` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `execution_id`           BIGINT       NOT NULL COMMENT '执行ID',
    `ecu_id`                 VARCHAR(64)  NOT NULL COMMENT 'ECU标识',
    `target_software_version` VARCHAR(64) NULL COMMENT '目标软件版本',
    `actual_software_version` VARCHAR(64) NULL COMMENT '实际软件版本',
    `result`                 VARCHAR(20)  NOT NULL COMMENT '结果：SUCCESS/FAILED/ROLLED_BACK',
    `fail_reason`            VARCHAR(500) NULL COMMENT '失败原因',
    `create_time`            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`              BIGINT       NULL COMMENT '创建人',
    `modify_time`            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`              BIGINT       NULL COMMENT '修改人',
    `row_version`            BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`              TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_ecu_result_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行ECU结果（CR-012）';

-- ============================================================
-- 13. tb_outbox_message: 事务性 Outbox（本地轻量实现）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_outbox_message` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `aggregate_type` VARCHAR(30)  NOT NULL COMMENT '聚合类型：TASK/VEHICLE_TASK/EXECUTION',
    `aggregate_id`   VARCHAR(64)  NOT NULL COMMENT '聚合ID',
    `event_type`     VARCHAR(64)  NOT NULL COMMENT '事件类型',
    `payload_json`   TEXT         NOT NULL COMMENT '事件负载JSON',
    `occurred_at`    DATETIME(3)  NOT NULL COMMENT '事件发生时间',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PUBLISHED/FAILED',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `last_error`     VARCHAR(500) NULL COMMENT '最近失败原因',
    `published_at`   DATETIME(3)  NULL COMMENT '投递时间',
    `create_time`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_outbox_status_occurred` (`status`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务性Outbox消息（CR-012）';

-- ============================================================
-- 14. tb_idempotency_record: 幂等记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_idempotency_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operation_scope`   VARCHAR(50)  NOT NULL COMMENT '操作作用域',
    `idempotency_key`   VARCHAR(128) NOT NULL COMMENT '幂等键',
    `request_digest`    VARCHAR(128) NOT NULL COMMENT '请求摘要',
    `response_snapshot` TEXT         NULL COMMENT '响应快照（JSON）',
    `vin`               VARCHAR(20)  NULL COMMENT '车架号',
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_scope_key` (`operation_scope`, `idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录（CR-012）';

-- ============================================================
-- 15. ALTER tb_task_vehicle: 新增 VehicleTask 字段（不删旧列，兼容）
-- ============================================================
ALTER TABLE `tb_task_vehicle`
    ADD COLUMN `vehicle_task_status` VARCHAR(30)  NULL COMMENT '车辆任务状态（CR-012）' AFTER `state`,
    ADD COLUMN `task_revision`      BIGINT       NULL DEFAULT 1 COMMENT '任务版本号（CR-012）' AFTER `vehicle_task_status`,
    ADD COLUMN `snapshot_digest`    VARCHAR(128) NULL COMMENT '快照摘要（CR-012）' AFTER `task_revision`,
    ADD COLUMN `availability_status` VARCHAR(30) NULL COMMENT '可用性状态（CR-012）' AFTER `snapshot_digest`,
    ADD COLUMN `download_ready_state` VARCHAR(30) NULL COMMENT '下载准备状态（CR-012）' AFTER `availability_status`,
    ADD COLUMN `consent_state`      VARCHAR(30)  NULL COMMENT '授权状态（CR-012）' AFTER `download_ready_state`,
    ADD COLUMN `release_at`         DATETIME(3)  NULL COMMENT '发布时间快照（CR-012）',
    ADD COLUMN `vt_start_time`      DATETIME(3)  NULL COMMENT '执行窗口开始时间快照（CR-012）',
    ADD COLUMN `vt_end_time`        DATETIME(3)  NULL COMMENT '执行窗口结束时间快照（CR-012）',
    ADD COLUMN `last_attempt_no`    INT          NULL DEFAULT 0 COMMENT '最近尝试序号（CR-012）',
    ADD COLUMN `active_execution_id` BIGINT      NULL COMMENT '活动执行ID（CR-012）' AFTER `last_attempt_no`,
    ADD COLUMN `superseded_by`      BIGINT       NULL COMMENT '取代者车辆任务ID（CR-012）',
    ADD COLUMN `local_disposition`  VARCHAR(30)  NULL COMMENT '本地任务处置意图（CR-012）',
    ADD COLUMN `package_cache_action` VARCHAR(30) NULL COMMENT '包缓存处置意图（CR-012）',
    ADD COLUMN `vt_state_before_pause` VARCHAR(30) NULL COMMENT '暂停前状态（CR-012）';

-- ============================================================
-- 16. ALTER tb_upgrade_log: 新增日志字段（重构）
-- ============================================================
ALTER TABLE `tb_upgrade_log`
    ADD COLUMN `log_request_id`     VARCHAR(64)  NULL COMMENT '日志上传申请ID（CR-012）' AFTER `vin`,
    ADD COLUMN `log_scope`          VARCHAR(50)  NULL COMMENT '采集范围（CR-012）' AFTER `log_request_id`,
    ADD COLUMN `desensitize_version` VARCHAR(30) NULL COMMENT '脱敏版本（CR-012）' AFTER `log_scope`,
    ADD COLUMN `object_key`         VARCHAR(512) NULL COMMENT '对象存储键（CR-012）' AFTER `desensitize_version`,
    ADD COLUMN `log_digest`         VARCHAR(128) NULL COMMENT '日志摘要（CR-012）' AFTER `object_key`,
    ADD COLUMN `upload_result`      VARCHAR(20)  NULL COMMENT '上传结果（CR-012）' AFTER `log_digest`;
