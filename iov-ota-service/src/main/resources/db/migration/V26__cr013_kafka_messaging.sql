-- CR-013: 无兼容 Kafka 消息化重构
-- 车端 CGW-FOTA <-> 车云接入层(MQTT) <-> IOV-OTA(Kafka)
-- IOV-OTA 仅通过 Kafka 消费上行事件并生产下行结果/命令，不直连 MQTT，不提供 /ccp/fota/** Controller。
-- 删除 CCP REST 时代的 HTTP 幂等表、旧 process/state 表与旧状态列。

-- ============================================================
-- 1. tb_kafka_message_inbox: 上行 Kafka 消息幂等与处理结果索引
--    UK(consumer_name, business_key) 保证同业务键只处理一次；
--    保存 payload_digest 以区分「同键同摘要(幂等)」与「同键异摘要(冲突)」。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_kafka_message_inbox` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `consumer_name`     VARCHAR(64)  NOT NULL COMMENT '消费者/消息类型标识（=messageType）',
    `business_key`      VARCHAR(128) NOT NULL COMMENT '业务唯一键（幂等键/业务ID）',
    `message_id`        VARCHAR(64)  NOT NULL COMMENT '物理消息ID',
    `message_type`      VARCHAR(64)  NOT NULL COMMENT '消息类型',
    `schema_version`    INT          NOT NULL DEFAULT 1 COMMENT 'schema版本',
    `payload_digest`    VARCHAR(128) NOT NULL COMMENT 'payload摘要',
    `kafka_topic`       VARCHAR(128) NULL COMMENT '来源Topic',
    `kafka_partition`   INT          NULL COMMENT '分区',
    `kafka_offset`      BIGINT       NULL COMMENT '偏移量',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PROCESSED' COMMENT '处理状态：PROCESSED/CONFLICT/FAILED/DLQ',
    `result_message_id` BIGINT       NULL COMMENT '生成的结果Outbox消息ID',
    `error_reason`      VARCHAR(500) NULL COMMENT '处理失败/冲突原因',
    `create_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kafka_inbox_consumer_business` (`consumer_name`, `business_key`),
    KEY `idx_kafka_inbox_message` (`message_id`),
    KEY `idx_kafka_inbox_offset` (`kafka_topic`, `kafka_partition`, `kafka_offset`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kafka上行消息Inbox（CR-013）';

-- ============================================================
-- 2. tb_kafka_message_outbox: 下行结果、命令和回执可靠生产
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_kafka_message_outbox` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `aggregate_type` VARCHAR(30)  NOT NULL COMMENT '聚合类型：TASK/VEHICLE_TASK/EXECUTION',
    `aggregate_id`   VARCHAR(64)  NOT NULL COMMENT '聚合ID',
    `message_type`   VARCHAR(64)  NOT NULL COMMENT '消息类型',
    `message_key`    VARCHAR(64)  NOT NULL COMMENT 'Kafka key（车辆级vin或Execution事件executionId）',
    `correlation_id` VARCHAR(64)  NULL COMMENT '关联请求correlationId',
    `vin`            VARCHAR(20)  NULL COMMENT '车架号',
    `payload_json`   TEXT         NOT NULL COMMENT '消息payload（JSON）',
    `publish_state`  VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '发布状态：PENDING/PUBLISHED/FAILED/DEAD',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at`  DATETIME(3)  NULL COMMENT '下次重试时间',
    `last_error`     VARCHAR(500) NULL COMMENT '最近失败原因',
    `published_at`   DATETIME(3)  NULL COMMENT '发布成功时间',
    `create_time`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_kafka_outbox_state_retry` (`publish_state`, `next_retry_at`),
    KEY `idx_kafka_outbox_type` (`message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kafka下行消息Outbox（CR-013）';

-- ============================================================
-- 3. 删除 CCP REST 时代的 HTTP 幂等表（CR-013 §6）
--    Kafka Inbox 已覆盖消息级幂等，不与 Inbox 重复保存投递事实。
-- ============================================================
DROP TABLE IF EXISTS `tb_idempotency_record`;

-- ============================================================
-- 4. 删除旧 v1 process 表（CR-013 §6.2，不纳入目标 schema）
-- ============================================================
DROP TABLE IF EXISTS `tb_task_vehicle_process`;

-- ============================================================
-- 5. 删除旧最小协议版本列（CR-013 §2.1，删除 minimumProtocolVersion 分支）
-- ============================================================
ALTER TABLE `tb_task`
    DROP COLUMN `minimum_protocol_version`;

-- ============================================================
-- 6. 删除旧 TaskVehicleState 状态列（CR-013 §6.2，不保留旧状态双写/并行字段）
--    新模型以 tb_task_vehicle.vehicle_task_status 为准。
-- ============================================================
ALTER TABLE `tb_task_vehicle`
    DROP COLUMN `state`;
