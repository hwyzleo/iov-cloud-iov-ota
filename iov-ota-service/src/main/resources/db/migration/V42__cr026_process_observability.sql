-- ============================================================
-- CR-026: 车辆升级全过程可观测性（前向迁移，仅静态 DDL）
-- ============================================================
-- 依据：IOV-OTA-DSN-CR-026（§6 数据模型、§7 核心流程、§8 查询契约）。
-- 约束：
--   1) 只新增表/可空列/索引，不改写任何历史迁移脚本，不删除既有列；
--   2) 存量数据一律保持 NULL，不用 create_time 伪造 occurredAt，不猜测回填；
--   3) 仅静态 DDL，禁止 PREPARE / SIGNAL / 存储过程（Druid WallFilter 约定）；
--   4) 新增表均带 BasePo 约定的 description/create_by/create_time/
--      modify_by/modify_time/row_version/row_valid 列，避免全字段 SELECT 缺列。
-- 与设计文档的两处实现期补列（解析见实现说明）：
--   a) tb_vehicle_task_package 追加 download grant 证据列：PAR-PROTO-DSN-CR-011
--      冻结「非零 RESET_OFFSET 基线必须命中最近一次授权 offset 且未过期」，
--      云端必须持久化最近一次 DownloadGrant 结果才能裁决；
--   b) tb_upgrade_log 追加 log_object_id：EDD-OSS 只提供按 objectId 查询/核验，
--      不存在按 requestId 反查的接口，OTA 必须保存授权返回的 objectId。
-- ============================================================

-- ============================================================
-- 1. tb_package_download_progress: 每下载尝试的当前进度快照（读模型）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_package_download_progress`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vehicle_task_id`      BIGINT       NOT NULL COMMENT '车辆任务ID',
    `package_id`           VARCHAR(64)  NOT NULL COMMENT '包ID',
    `package_revision`     VARCHAR(64)  NULL COMMENT '包版本号',
    `download_attempt_no`  INT          NOT NULL COMMENT '下载尝试序号（独立于安装 Execution attempt）',
    `latest_progress_id`   VARCHAR(64)  NULL COMMENT '最近受理的进度ID',
    `latest_progress_seq`  BIGINT       NOT NULL DEFAULT 0 COMMENT '当前快照进度序号',
    `download_state`       VARCHAR(20)  NOT NULL DEFAULT 'AUTHORIZED' COMMENT '下载状态：STARTED/PROGRESS/PAUSED/RESUMED/RESET_OFFSET/FAILED/COMPLETED',
    `downloaded_bytes`     BIGINT       NOT NULL DEFAULT 0 COMMENT '已下载字节（车端事实）',
    `total_bytes`          BIGINT       NOT NULL COMMENT '包总字节（权威包大小）',
    `resume_offset`        BIGINT       NOT NULL DEFAULT 0 COMMENT '续传起点（车端事实）',
    `progress_percent`     DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '云端按字节计算的百分比，不作为车端权威输入',
    `etag`                 VARCHAR(128) NULL COMMENT '当前对象ETag',
    `error_code`           VARCHAR(64)  NULL COMMENT '车端错误码（仅 FAILED）',
    `failure_reason`       VARCHAR(500) NULL COMMENT '车端失败原因（仅 FAILED）',
    `source_occurred_at`   DATETIME(3)  NULL COMMENT '车端发生时间（occurred_at_ms=0 时为 NULL，不伪造）',
    `received_at`          DATETIME(3)  NOT NULL COMMENT '云端受理时间',
    `description`          VARCHAR(255) NULL COMMENT '备注',
    `create_by`            VARCHAR(64)  NULL COMMENT '创建者',
    `create_time`          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_by`            VARCHAR(64)  NULL COMMENT '修改者',
    `modify_time`          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `row_version`          INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    `row_valid`            TINYINT      NOT NULL DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_download_progress_attempt` (`vehicle_task_id`, `package_id`, `download_attempt_no`),
    KEY `idx_download_progress_state` (`vehicle_task_id`, `package_id`, `download_state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='包下载进度当前快照（CR-026）';

-- ============================================================
-- 2. tb_package_download_progress_event: 不可变进度历史（降采样事实）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_package_download_progress_event`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `progress_id`          VARCHAR(64)  NOT NULL COMMENT '进度ID（全局第一幂等键）',
    `vehicle_task_id`      BIGINT       NOT NULL COMMENT '车辆任务ID',
    `package_id`           VARCHAR(64)  NOT NULL COMMENT '包ID',
    `package_revision`     VARCHAR(64)  NULL COMMENT '包版本号',
    `download_attempt_no`  INT          NOT NULL COMMENT '下载尝试序号',
    `progress_seq`         BIGINT       NOT NULL COMMENT '进度序号',
    `download_state`       VARCHAR(20)  NOT NULL COMMENT '下载状态',
    `downloaded_bytes`     BIGINT       NOT NULL COMMENT '已下载字节',
    `total_bytes`          BIGINT       NOT NULL COMMENT '包总字节',
    `resume_offset`        BIGINT       NOT NULL DEFAULT 0 COMMENT '续传起点',
    `progress_percent`     DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '云端计算百分比',
    `etag`                 VARCHAR(128) NULL COMMENT '对象ETag',
    `error_code`           VARCHAR(64)  NULL COMMENT '车端错误码',
    `failure_reason`       VARCHAR(500) NULL COMMENT '车端失败原因',
    `event_digest`         VARCHAR(128) NOT NULL COMMENT '事件摘要（幂等冲突判定）',
    `disposition`          VARCHAR(30)  NOT NULL COMMENT '受理处置：CURRENT/LATE_OBSERVATION',
    `source_occurred_at`   DATETIME(3)  NULL COMMENT '车端发生时间（未知为 NULL）',
    `received_at`          DATETIME(3)  NOT NULL COMMENT '云端受理时间',
    `description`          VARCHAR(255) NULL COMMENT '备注',
    `create_by`            VARCHAR(64)  NULL COMMENT '创建者',
    `create_time`          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_by`            VARCHAR(64)  NULL COMMENT '修改者',
    `modify_time`          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `row_version`          INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    `row_valid`            TINYINT      NOT NULL DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_download_progress_id` (`progress_id`),
    UNIQUE KEY `uk_download_progress_seq` (`vehicle_task_id`, `package_id`, `download_attempt_no`, `progress_seq`),
    KEY `idx_download_progress_page` (`vehicle_task_id`, `package_id`, `download_attempt_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='包下载进度不可变历史（CR-026）';

-- ============================================================
-- 3. tb_task_vehicle_execution: 车端起止时间对应的云端受理时间
-- ============================================================
ALTER TABLE `tb_task_vehicle_execution`
    ADD COLUMN `start_received_time` DATETIME(3) NULL COMMENT '车端安装开始时间对应的云端受理时间（CR-026）' AFTER `start_time`,
    ADD COLUMN `end_received_time`   DATETIME(3) NULL COMMENT '车端安装结束时间对应的云端受理时间（CR-026）' AFTER `end_time`;

-- ============================================================
-- 4. tb_task_vehicle_execution_event: 事件白名单结构化列
-- ============================================================
ALTER TABLE `tb_task_vehicle_execution_event`
    ADD COLUMN `source_occurred_at`      DATETIME(3)   NULL COMMENT '车端事件发生时间（occurred_at_ms=0 为 NULL）' AFTER `disposition`,
    ADD COLUMN `progress_percent`        INT           NULL COMMENT '事件携带的百分比（0..100）' AFTER `source_occurred_at`,
    ADD COLUMN `ecu_id`                  VARCHAR(64)   NULL COMMENT 'ECU标识' AFTER `progress_percent`,
    ADD COLUMN `software_target_code`    VARCHAR(64)   NULL COMMENT '软件目标标识（MULTI_TARGET）' AFTER `ecu_id`,
    ADD COLUMN `slot`                    VARCHAR(32)   NULL COMMENT '槽位' AFTER `software_target_code`,
    ADD COLUMN `error_code`              VARCHAR(64)   NULL COMMENT '错误码' AFTER `slot`,
    ADD COLUMN `failure_reason`          VARCHAR(500)  NULL COMMENT '错误详情（截断）' AFTER `error_code`;

-- ============================================================
-- 5. tb_package_stage_result: 下载尝试、车端发生时间与云端受理时间
-- ============================================================
ALTER TABLE `tb_package_stage_result`
    ADD COLUMN `download_attempt_no` INT          NULL COMMENT '下载尝试序号（可空，历史数据未知）' AFTER `fail_reason`,
    ADD COLUMN `source_occurred_at`  DATETIME(3)  NULL COMMENT '车端发生时间（未知为 NULL）' AFTER `download_attempt_no`,
    ADD COLUMN `received_at`         DATETIME(3)  NULL COMMENT '云端受理时间（历史数据不回填）' AFTER `source_occurred_at`,
    ADD KEY `idx_stage_result_page` (`vehicle_task_id`, `package_id`, `id`);

-- ============================================================
-- 6. tb_upgrade_log: 日志对象登记事实（VehicleTask/Execution 主锚点）
-- ============================================================
ALTER TABLE `tb_upgrade_log`
    ADD COLUMN `vehicle_task_id`    BIGINT       NULL COMMENT '车辆任务ID（过程关联主锚点，CR-026）' AFTER `vin`,
    ADD COLUMN `execution_id`       BIGINT       NULL COMMENT '安装执行ID（可空，CR-026）' AFTER `vehicle_task_id`,
    ADD COLUMN `log_object_id`      VARCHAR(36)  NULL COMMENT 'EDD-OSS 日志对象ID（complete/查询必需）' AFTER `object_key`,
    ADD COLUMN `privacy_level`      VARCHAR(32)  NULL COMMENT '隐私分级（CR-026）' AFTER `log_object_id`,
    ADD COLUMN `object_size_bytes`  BIGINT       NULL COMMENT '对象声明/实际字节数（CR-026）' AFTER `privacy_level`,
    ADD COLUMN `digest_algorithm`   VARCHAR(16)  NULL COMMENT '摘要算法（CR-026，sha-256）' AFTER `object_size_bytes`,
    ADD COLUMN `failure_reason`     VARCHAR(128) NULL COMMENT '稳定失败原因码（CR-026）' AFTER `upload_result`,
    ADD COLUMN `authorized_at`      DATETIME(3)  NULL COMMENT '授权登记时间（CR-026）' AFTER `upload_time`,
    ADD COLUMN `uploaded_at`        DATETIME(3)  NULL COMMENT '对象核验通过时间（CR-026）' AFTER `authorized_at`,
    ADD COLUMN `received_at`        DATETIME(3)  NULL COMMENT '结果受理时间（CR-026）' AFTER `uploaded_at`,
    ADD UNIQUE KEY `uk_upgrade_log_request` (`log_request_id`),
    ADD KEY `idx_upgrade_log_vt` (`vehicle_task_id`, `execution_id`, `id`);

-- ============================================================
-- 7. tb_task_vehicle_retry_log: 业务重试事实（幂等键 + 关联 + 时间）
-- ============================================================
ALTER TABLE `tb_task_vehicle_retry_log`
    ADD COLUMN `retry_event_id`     VARCHAR(64)  NULL COMMENT '重试事件ID（业务消息身份幂等键，CR-026）' AFTER `vin`,
    ADD COLUMN `vehicle_task_id`    BIGINT       NULL COMMENT '车辆任务ID（CR-026）' AFTER `retry_event_id`,
    ADD COLUMN `action`             VARCHAR(30)  NULL COMMENT '动作：RETRY/RESUME/RESET_OFFSET/EXECUTION_CREATED（CR-026）' AFTER `stage`,
    ADD COLUMN `error_code`         VARCHAR(64)  NULL COMMENT '错误码（CR-026）' AFTER `action`,
    ADD COLUMN `source_occurred_at` DATETIME(3)  NULL COMMENT '车端发生时间（未知为 NULL，CR-026）' AFTER `retried_at`,
    ADD COLUMN `received_at`        DATETIME(3)  NULL COMMENT '云端受理时间（CR-026）' AFTER `source_occurred_at`,
    ADD UNIQUE KEY `uk_retry_event_id` (`retry_event_id`),
    ADD KEY `idx_retry_log_page` (`vehicle_task_id`, `stage`, `attempt_no`, `id`);

-- ============================================================
-- 8. tb_vehicle_task_package: 最近一次下载授权证据（RESET_OFFSET 裁决）
-- ============================================================
ALTER TABLE `tb_vehicle_task_package`
    ADD COLUMN `grant_offset`           BIGINT      NULL COMMENT '最近一次授权返回的 current_offset_bytes（CR-026）' AFTER `verify_state`,
    ADD COLUMN `grant_resume_allowed`   TINYINT     NULL COMMENT '最近一次授权 resume_allowed（CR-026）' AFTER `grant_offset`,
    ADD COLUMN `grant_etag`             VARCHAR(128) NULL COMMENT '授权时的对象ETag（CR-026）' AFTER `grant_resume_allowed`,
    ADD COLUMN `grant_package_revision` VARCHAR(64) NULL COMMENT '授权时的包版本号（CR-026）' AFTER `grant_etag`,
    ADD COLUMN `granted_at`             DATETIME(3) NULL COMMENT '授权时间（CR-026）' AFTER `grant_package_revision`,
    ADD COLUMN `grant_expires_at`       DATETIME(3) NULL COMMENT '授权凭证过期时间（CR-026）' AFTER `granted_at`;

-- ============================================================
-- 9. tb_idempotency_record: 下载进度业务幂等处置存档（无新表）
-- ============================================================
-- 说明：PAR-PROTO-DSN-CR-011 冻结「REJECTED/CONFLICT 只保留 progressId 首次处置用于
-- 幂等回放，不占用 (vehicle_task_id,package_id,download_attempt_no,progress_seq)」。
-- 为不改写既有唯一键语义，首次处置（含拒绝原因/响应快照）统一写入平台既有
-- tb_idempotency_record（operation_scope='DOWNLOAD_PROGRESS'），进度历史表仅保存
-- CURRENT / LATE_OBSERVATION 事实。
CREATE INDEX `idx_idempotency_created` ON `tb_idempotency_record` (`operation_scope`, `create_time`);
