-- CR-014: PAR-PROTO 强类型协议接入
-- 设计增量见 IOV-OTA-SPEC设计 §4.18 / IOV-OTA-DSN-CR-014。
-- 原则：不再新写旧 JSON/messageType/schemaVersion 字段；旧列仅作为 legacy 保留，
--       由后续 Flyway 安全删除，不作为兼容读取源。

-- ============================================================
-- 1. tb_kafka_message_inbox 重构
--    UK(consumer_name, message_id) + envelope_sha256 幂等；
--    旧 business_key/message_type/schema_version/payload_digest 改为 legacy_*（禁止新写）。
-- ============================================================
ALTER TABLE `tb_kafka_message_inbox`
    DROP INDEX `uk_kafka_inbox_consumer_business`,
    CHANGE COLUMN `business_key`  `legacy_business_key`  VARCHAR(128) NULL COMMENT 'legacy(CR-013) 业务键，禁止新写',
    CHANGE COLUMN `message_type`  `legacy_message_type`  VARCHAR(64)  NULL COMMENT 'legacy(CR-013) 消息类型，禁止新写',
    CHANGE COLUMN `schema_version` `legacy_schema_version` INT NULL COMMENT 'legacy(CR-013) schema版本，禁止新写',
    CHANGE COLUMN `payload_digest` `legacy_payload_digest` VARCHAR(128) NULL COMMENT 'legacy(CR-013) payload摘要，禁止新写',
    ADD COLUMN `envelope_sha256` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'raw Envelope SHA-256',
    ADD COLUMN `payload_type`    VARCHAR(128) NOT NULL DEFAULT '' COMMENT '全限定 payload_type（vehicle.fota.v1.*）',
    ADD COLUMN `message_kind`    VARCHAR(20)  NOT NULL DEFAULT '' COMMENT 'REQUEST/RESPONSE/EVENT',
    ADD COLUMN `protocol_version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'protocol version',
    ADD COLUMN `vin`             VARCHAR(20) NULL COMMENT '车架号（仅协议承载）',
    ADD UNIQUE KEY `uk_kafka_inbox_consumer_message` (`consumer_name`, `message_id`);

-- ============================================================
-- 2. tb_kafka_message_outbox 重构
--    首次创建即冻结完整 Envelope bytes；重试复用 bytes；
--    旧 message_type/message_key/payload_json 改为 legacy_*（禁止新写）。
-- ============================================================
ALTER TABLE `tb_kafka_message_outbox`
    CHANGE COLUMN `message_type` `legacy_message_type` VARCHAR(64)  NULL COMMENT 'legacy(CR-013) 消息类型，禁止新写',
    CHANGE COLUMN `message_key`  `legacy_message_key`  VARCHAR(64)  NULL COMMENT 'legacy(CR-013) Kafka key，禁止新写',
    CHANGE COLUMN `payload_json` `legacy_payload_json` TEXT         NULL COMMENT 'legacy(CR-013) JSON payload，禁止新写',
    ADD COLUMN `message_id`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '传输消息唯一 ID',
    ADD COLUMN `payload_type`   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '全限定 payload_type（vehicle.fota.v1.*）',
    ADD COLUMN `message_kind`   VARCHAR(20)  NOT NULL DEFAULT '' COMMENT 'REQUEST/RESPONSE/EVENT',
    ADD COLUMN `envelope_bytes` LONGBLOB NULL COMMENT '冻结的完整序列化 Envelope bytes（重试复用）',
    ADD COLUMN `envelope_sha256` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Envelope bytes SHA-256';

-- ============================================================
-- 3. 新增 tb_gateway_delivery_observation（VAGW 云内技术投递观测）
--    UK(original_message_id, stage, occurred_at_ms)；VIN 仅存 hash，不落原文。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_gateway_delivery_observation` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `original_message_id` VARCHAR(64) NOT NULL COMMENT '原 FOTA 传输消息 message_id',
    `correlation_id`     VARCHAR(64)  NULL COMMENT '关联 correlation_id',
    `vin_hash`           VARCHAR(64)  NOT NULL COMMENT 'VIN SHA-256（不存原文）',
    `stage`              VARCHAR(64)  NULL COMMENT '技术阶段，如 DOWNLINK_RECEIVED/MQTT_PUBLISHED',
    `outcome`            VARCHAR(20)  NOT NULL COMMENT 'OUTCOME_ACCEPTED/REJECTED/UNKNOWN',
    `reason`             VARCHAR(128) NULL COMMENT '原因码，如 VEHICLE_OFFLINE/VIN_UNBOUND/...',
    `retryable`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否可重试',
    `retry_after_ms`     BIGINT       NULL COMMENT '建议重试间隔（毫秒）',
    `occurred_at_ms`     BIGINT       NOT NULL COMMENT '技术投递发生时间（毫秒）',
    `received_at`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'IOV-OTA 收到时间',
    `create_time`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_observation` (`original_message_id`, `stage`, `occurred_at_ms`),
    KEY `idx_delivery_observation_vin_hash` (`vin_hash`),
    KEY `idx_delivery_observation_outcome` (`outcome`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VAGW 云内技术投递状态观测（CR-014）';
