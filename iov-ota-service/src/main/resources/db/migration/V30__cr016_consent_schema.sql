-- CR-016: Consent 收口为 VehicleTask 子实体
-- 设计增量见 IOV-OTA-DSN-CR-016 §3 / §7。
-- 原则：tb_vehicle_task_consent 是唯一授权事实表（追加历史，不覆盖）；
--       tb_task_vehicle 保存当前授权状态/当前记录引用/冻结条款快照，供许可门禁强一致读取；
--       旧独立 UserConsent 模型（tb_user_consent）无历史数据，直接删除，不做数据迁移。

-- ============================================================
-- 1. 重建 tb_vehicle_task_consent 为 CR-016 唯一授权事实表
--    （V24 CR-012 旧 schema 与 CR-016 差异过大且无存量数据，直接重建）
-- ============================================================
DROP TABLE IF EXISTS `tb_vehicle_task_consent`;

CREATE TABLE IF NOT EXISTS `tb_vehicle_task_consent` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vehicle_task_id`       BIGINT       NOT NULL COMMENT '车辆任务ID（逻辑引用 tb_task_vehicle.id）',
    `task_id`               BIGINT       NOT NULL COMMENT '冗余任务ID快照，便于审计与分区查询',
    `vin`                   VARCHAR(64)  NOT NULL COMMENT '冗余车架号快照，敏感展示按策略脱敏',
    `task_revision`         BIGINT       NOT NULL COMMENT '消息所针对的任务修订，必填',
    `consent_result`        VARCHAR(32)  NOT NULL COMMENT '业务结果：GRANTED/REJECTED/REVOKED',
    `consent_receipt_id`    VARCHAR(128) NULL COMMENT '授权回执ID，GRANTED 必填且全局唯一',
    `supersedes_consent_id` BIGINT       NULL COMMENT '重新授权/撤回所取代的前一条记录ID',
    `article_id`            BIGINT       NULL COMMENT '条款/须知身份引用',
    `article_version`       VARCHAR(64)  NULL COMMENT '条款展示版本快照',
    `article_hash`          VARCHAR(128) NULL COMMENT '条款权威正文摘要，需同意时必填',
    `consent_scope_digest`  VARCHAR(128) NOT NULL COMMENT '绑定任务修订、条款集合和受控动作范围的摘要',
    `channel`               VARCHAR(32)  NULL COMMENT '上报渠道：HMI/TBOX/APP 等',
    `subject_ref`           VARCHAR(128) NULL COMMENT '授权主体引用（协议可提供，不作聚合归属键）',
    `reported_at`           DATETIME(3)  NULL COMMENT '车端业务时间',
    `received_at`           DATETIME(3)  NOT NULL COMMENT '云端接收时间',
    `expire_at`             DATETIME(3)  NULL COMMENT '可选有效期',
    `message_id`            VARCHAR(128) NOT NULL COMMENT 'Kafka Envelope 消息ID',
    `idempotency_key`       VARCHAR(128) NULL COMMENT '写入幂等键',
    `request_digest`        VARCHAR(128) NOT NULL COMMENT '同键异参冲突检测',
    `source_model`          VARCHAR(32)  NOT NULL DEFAULT 'NATIVE' COMMENT '来源：NATIVE/MIGRATED_USER_CONSENT',
    `create_time`           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`             BIGINT       NULL COMMENT '创建人',
    `modify_time`           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`             BIGINT       NULL COMMENT '修改人',
    `row_version`           BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`             TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_task_consent_message` (`message_id`),
    UNIQUE KEY `uk_vehicle_task_consent_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_vehicle_task_consent_receipt` (`consent_receipt_id`),
    KEY `idx_vehicle_task_consent_history` (`vehicle_task_id`, `received_at`, `id`),
    KEY `idx_task_consent_query` (`task_id`, `consent_result`, `received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆任务授权唯一事实表（CR-016）';

-- ============================================================
-- 2. tb_task_vehicle：当前授权状态 + 当前记录引用 + 发布冻结条款快照
--    consent_state 已由 CR-012 提供；新增其余字段。
-- ============================================================
ALTER TABLE `tb_task_vehicle`
    ADD COLUMN `consent_required`     TINYINT      NULL DEFAULT 0 COMMENT '发布时冻结，不随活动后续编辑漂移（CR-016）' AFTER `consent_state`,
    ADD COLUMN `consent_article_id`   BIGINT       NULL COMMENT '发布冻结条款身份引用（CR-016）' AFTER `consent_required`,
    ADD COLUMN `consent_article_version` VARCHAR(64) NULL COMMENT '发布冻结条款展示版本（CR-016）' AFTER `consent_article_id`,
    ADD COLUMN `consent_article_hash` VARCHAR(128) NULL COMMENT '发布冻结条款权威摘要（CR-016）' AFTER `consent_article_version`,
    ADD COLUMN `consent_scope_digest` VARCHAR(128) NULL COMMENT '当前任务修订、条款和授权动作范围的权威摘要（CR-016）' AFTER `consent_article_hash`,
    ADD COLUMN `current_consent_id`   BIGINT       NULL COMMENT '指向当前权威授权记录（CR-016）' AFTER `consent_scope_digest`,
    ADD COLUMN `consent_updated_at`   DATETIME(3)  NULL COMMENT '当前授权状态最后推进时间（CR-016）' AFTER `current_consent_id`;

-- ============================================================
-- 3. 删除旧独立 UserConsent 模型表（无历史数据，不迁移，不双写）
-- ============================================================
DROP TABLE IF EXISTS `tb_user_consent`;
