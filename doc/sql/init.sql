DROP TABLE IF EXISTS `db_pota`.`tb_vehicle_part`;
CREATE TABLE `db_pota`.`tb_vehicle_part`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pn`            VARCHAR(20) NOT NULL COMMENT '零件编号',
    `vin`           VARCHAR(20) NOT NULL COMMENT '车架号',
    `device_code`   VARCHAR(20)          DEFAULT NULL COMMENT '设备代码',
    `device_item`   VARCHAR(20)          DEFAULT NULL COMMENT '设备项',
    `sn`            VARCHAR(255)         DEFAULT NULL COMMENT '零件序列号',
    `config_word`   VARCHAR(255)         DEFAULT NULL COMMENT '配置字',
    `supplier_code` VARCHAR(255)         DEFAULT NULL COMMENT '供应商编码',
    `batch_num`     VARCHAR(255)         DEFAULT NULL COMMENT '批次号',
    `hardware_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '软件版本号',
    `hardware_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '硬件零件号',
    `software_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '软件零件号',
    `extra`         TEXT                 DEFAULT NULL COMMENT '附加信息',
    `description`   VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`),
    INDEX `idx_pn` (`pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零件表';

DROP TABLE IF EXISTS `db_pota`.`tb_vehicle_part_history`;
CREATE TABLE `db_pota`.`tb_vehicle_part_history`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pn`            VARCHAR(20) NOT NULL COMMENT '零件编号',
    `vin`           VARCHAR(20)          DEFAULT NULL COMMENT '车架号',
    `device_code`   VARCHAR(20)          DEFAULT NULL COMMENT '设备代码',
    `device_item`   VARCHAR(20)          DEFAULT NULL COMMENT '设备项',
    `sn`            VARCHAR(255)         DEFAULT NULL COMMENT '零件序列号',
    `config_word`   VARCHAR(255)         DEFAULT NULL COMMENT '配置字',
    `supplier_code` VARCHAR(255)         DEFAULT NULL COMMENT '供应商编码',
    `batch_num`     VARCHAR(255)         DEFAULT NULL COMMENT '批次号',
    `hardware_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '软件版本号',
    `hardware_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '硬件零件号',
    `software_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '软件零件号',
    `extra`         TEXT                 DEFAULT NULL COMMENT '附加信息',
    `description`   VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`),
    INDEX `idx_pn` (`pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零件变更历史表';