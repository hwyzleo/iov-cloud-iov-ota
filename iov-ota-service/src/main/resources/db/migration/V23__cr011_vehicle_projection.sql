-- CR-011: 车辆主档本地只读投影
-- 消费 MDM/VMD VehicleProduceEvent，以 VIN + 上游版本幂等 upsert

CREATE TABLE `tb_vehicle_projection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin` VARCHAR(20) NOT NULL COMMENT '车架号，车辆投影业务主键',
    `production_time` DATETIME(3) NOT NULL COMMENT '车辆生产业务事实时间',

    `plant_code` VARCHAR(64) NULL COMMENT '生产工厂编码',
    `brand_code` VARCHAR(64) NULL COMMENT '品牌编码',
    `platform_code` VARCHAR(64) NULL COMMENT '平台编码',
    `car_line_code` VARCHAR(64) NULL COMMENT '车系编码',
    `model_code` VARCHAR(64) NULL COMMENT '车型编码',
    `variant_code` VARCHAR(64) NULL COMMENT '版本编码',
    `configuration_code` VARCHAR(64) NULL COMMENT '配置编码，OTA基线和圈车核心锚点',

    `source_event_id` VARCHAR(64) NOT NULL COMMENT '最近一次生效的上游事件ID',
    `source_version` BIGINT NOT NULL COMMENT 'MDM/VMD车辆主档版本',
    `source_event_time` DATETIME(3) NOT NULL COMMENT '上游事件发生时间',
    `last_sync_time` DATETIME(3) NOT NULL COMMENT '投影最后同步时间',

    `description` VARCHAR(500) NULL COMMENT '描述',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by` BIGINT NULL COMMENT '创建人',
    `modify_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by` BIGINT NULL COMMENT '修改人',
    `row_version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_projection_vin` (`vin`),
    KEY `idx_vehicle_projection_production_time` (`production_time`),
    KEY `idx_vehicle_projection_configuration` (`configuration_code`, `production_time`),
    KEY `idx_vehicle_projection_variant` (`variant_code`, `production_time`),
    KEY `idx_vehicle_projection_source_version` (`source_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA车辆主档本地只读投影';
