-- CR-019: 统一 FOTA 软件清单与全链路适配
-- 设计增量见 IOV-OTA-DSN-CR-019 §5 / §10，主设计文档 §4.23.3。
-- 原则：
--   1) tb_vehicle_inventory 扩展 canonicalization_version / canonical_digest / source_collected_at；
--      accepted_time 继续表示服务端成功受理时间，不得用 collectedAt 替代。
--   2) tb_vehicle_inventory_item 继续一行一个 ECU，扩展 software_model / legacy_slot / legacy_active；
--      legacy 软件单值列仅保留只读兼容，新业务不得以 legacy 列为唯一来源。
--   3) 新建 tb_vehicle_inventory_software_unit 子表（RD-019-3：多 Target 使用独立子表，
--      不以 JSON 列替代关系模型）；SINGLE_IMAGE 也写一条 Target=ECU_IMAGE。
--   4) Execution ECU 结果扩展 software_model，并新建 per-Target/Slot 子表，
--      与 inventory SoftwareUnit 结构对称（§4.23.5）。

-- ============================================================
-- 1. tb_vehicle_inventory 增量列
-- ============================================================
ALTER TABLE `tb_vehicle_inventory`
    ADD COLUMN `canonicalization_version` INT NOT NULL DEFAULT 1 COMMENT 'canonicalization 版本（v1/v2）' AFTER `algorithm`,
    ADD COLUMN `canonical_digest` VARBINARY(32) NULL COMMENT 'canonicalization-v2 摘要（SHA-256）' AFTER `canonicalization_version`,
    ADD COLUMN `source_collected_at` DATETIME(3) NULL COMMENT '车端清单采集时间（reported_at，与服务端 accepted_time 分列）' AFTER `canonical_digest`;

-- ============================================================
-- 2. tb_vehicle_inventory_item 增量列
-- ============================================================
ALTER TABLE `tb_vehicle_inventory_item`
    ADD COLUMN `software_model` VARCHAR(32) NULL COMMENT 'ECU 软件模型：SINGLE_IMAGE/MULTI_TARGET' AFTER `hardware_version`,
    ADD COLUMN `legacy_slot` VARCHAR(32) NULL COMMENT 'legacy SINGLE_IMAGE 槽位（只读兼容）' AFTER `software_model`,
    ADD COLUMN `legacy_active` TINYINT(1) NULL COMMENT 'legacy SINGLE_IMAGE 活动槽（只读兼容，缺省 true）' AFTER `legacy_slot`;

-- ============================================================
-- 3. 新建 tb_vehicle_inventory_software_unit
--    UK(inventory_item_id, software_target_code, slot_key)
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_inventory_software_unit` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inventory_id`          BIGINT       NOT NULL COMMENT '清单头ID',
    `inventory_item_id`     BIGINT       NOT NULL COMMENT '清单明细ID',
    `vin`                   VARCHAR(20)  NOT NULL COMMENT '车架号',
    `ecu_id`                VARCHAR(128) NOT NULL COMMENT 'ECU标识',
    `software_target_code`  VARCHAR(128) NOT NULL COMMENT '软件目标编码（存量规范化为 ECU_IMAGE）',
    `software_part_number`  VARCHAR(128) NOT NULL COMMENT '软件零件号',
    `sw_version`            VARCHAR(128) NOT NULL COMMENT '软件版本',
    `slot`                  VARCHAR(32)  NULL COMMENT '运行槽位（A/B；空=非 A/B 或缺省当前镜像）',
    `slot_key`              VARCHAR(32)  GENERATED ALWAYS AS (COALESCE(`slot`, '')) STORED COMMENT '槽位排序/唯一键',
    `active`                TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否活动槽',
    `digest`                VARCHAR(256) NULL COMMENT '软件单元内容摘要（可空）',
    `create_time`           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`             BIGINT       NULL COMMENT '创建人',
    `modify_time`           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`             BIGINT       NULL COMMENT '修改人',
    `row_version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`             TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inventory_unit` (`inventory_item_id`, `software_target_code`, `slot_key`),
    KEY `idx_inventory_unit_inventory` (`inventory_id`),
    KEY `idx_vin_target` (`vin`, `ecu_id`, `software_target_code`),
    CONSTRAINT `fk_inventory_unit_item` FOREIGN KEY (`inventory_item_id`)
        REFERENCES `tb_vehicle_inventory_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆ECU清单软件单元子表（CR-019）';

-- ============================================================
-- 4. tb_task_vehicle_execution_ecu_result 增量列 + 子表
--    ECU 行保留聚合结果；per-Target/Slot 单元结果落子表（SoftwareUnit 结构）。
-- ============================================================
ALTER TABLE `tb_task_vehicle_execution_ecu_result`
    ADD COLUMN `software_model` VARCHAR(32) NULL COMMENT 'ECU 软件模型：SINGLE_IMAGE/MULTI_TARGET' AFTER `ecu_id`;

CREATE TABLE IF NOT EXISTS `tb_task_vehicle_execution_ecu_software_unit` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ecu_result_id`            BIGINT       NOT NULL COMMENT 'ECU 结果ID',
    `execution_id`             BIGINT       NOT NULL COMMENT '执行ID',
    `ecu_id`                   VARCHAR(64)  NOT NULL COMMENT 'ECU标识',
    `software_target_code`     VARCHAR(128) NOT NULL COMMENT '软件目标编码',
    `source_version`           VARCHAR(64)  NULL COMMENT '源版本',
    `target_version`           VARCHAR(64)  NULL COMMENT '目标版本',
    `actual_version`           VARCHAR(64)  NULL COMMENT '实际版本',
    `slot`                     VARCHAR(32)  NULL COMMENT '运行槽位',
    `active`                   TINYINT(1)   NULL COMMENT '是否活动槽',
    `result`                   VARCHAR(20)  NOT NULL COMMENT '结果：SUCCESS/FAILED/ROLLED_BACK',
    `failure_stage`            VARCHAR(64)  NULL COMMENT '失败阶段',
    `rollback_result`          VARCHAR(20)  NULL COMMENT '回滚结果：SUCCESS/FAILED/ROLLED_BACK',
    `package_id`               VARCHAR(64)  NULL COMMENT '关联软件包ID',
    `create_time`              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`                BIGINT       NULL COMMENT '创建人',
    `modify_time`              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`                BIGINT       NULL COMMENT '修改人',
    `row_version`              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`                TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_ecu_unit_result` (`ecu_result_id`),
    KEY `idx_ecu_unit_exec` (`execution_id`, `ecu_id`, `software_target_code`),
    CONSTRAINT `fk_ecu_unit_result` FOREIGN KEY (`ecu_result_id`)
        REFERENCES `tb_task_vehicle_execution_ecu_result` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装执行ECU软件单元结果子表（CR-019）';

-- ============================================================
-- 5. 活动升级对象/软件包安装计划：可空 software_target_code（存量 NULL=ECU_IMAGE）
-- ============================================================
ALTER TABLE `tb_activity_upgrade_target`
    ADD COLUMN `software_target_code` VARCHAR(128) NULL COMMENT '软件目标编码（存量 NULL 规范化为 ECU_IMAGE）' AFTER `vehicle_node_code`;

ALTER TABLE `tb_software_build_version_package`
    ADD COLUMN `software_target_code` VARCHAR(128) NULL COMMENT '软件目标编码（存量 NULL 规范化为 ECU_IMAGE）' AFTER `software_package_id`;

-- ============================================================
-- 6. 新建 tb_cloud_event_outbox（OTA→VMD 云服务观测消息事务性 Outbox）
--    Topic: ota.vehicle-software-inventory.observed，Key=VIN，DLQ 为 .dlq。
--    云服务消息不使用 Proto、不归 PAR-PROTO 管理（§4.23.6）。
--    UK(business_key) 保证相同观测身份不重复建 Outbox。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_cloud_event_outbox` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_type`    VARCHAR(64)  NOT NULL COMMENT '事件类型：VEHICLE_INVENTORY_OBSERVED',
    `business_key`  VARCHAR(128) NOT NULL COMMENT '业务唯一键（VEHICLE_INVENTORY_OBSERVED + observation_key）',
    `payload_json`  TEXT         NOT NULL COMMENT '事件负载 JSON（VehicleSoftwareInventoryObservedEvent）',
    `topic`         VARCHAR(128) NOT NULL DEFAULT 'ota.vehicle-software-inventory.observed' COMMENT '目标 Topic',
    `vin`           VARCHAR(20)  NULL COMMENT 'Kafka Key（VIN）',
    `publish_state` VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHING/PUBLISHED/FAILED/DEAD',
    `retry_count`   INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at` DATETIME(3)  NULL COMMENT '下次重试时间',
    `last_error`    VARCHAR(500) NULL COMMENT '最近错误',
    `published_at`  DATETIME(3)  NULL COMMENT '发布成功时间',
    `create_time`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `modify_time`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cloud_event_business_key` (`business_key`),
    KEY `idx_cloud_event_state_retry` (`publish_state`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA→VMD 云服务事件 Outbox（CR-019）';
