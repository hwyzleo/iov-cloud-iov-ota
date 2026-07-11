-- Fix: tb_software_package was pre-existing from old POTA schema with incompatible columns
-- (e.g. series_code, ecu_code, file_name, file_code, file_size, file_md5, etc.).
-- V5 and V6 added missing new columns, but old NOT NULL columns without defaults
-- (like series_code) still block INSERTs that don't reference them.
-- Solution: drop and recreate with the correct V1 DDL.

DROP TABLE IF EXISTS `tb_software_package`;

CREATE TABLE `tb_software_package`
(
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_code`            VARCHAR(20)  NOT NULL COMMENT '设备编码',
    `software_pn`            VARCHAR(50)  NOT NULL COMMENT '软件零件号',
    `package_name`           VARCHAR(255) NOT NULL COMMENT '软件包名称',
    `package_code`           VARCHAR(255)          DEFAULT NULL COMMENT '软件包代码',
    `package_url`            VARCHAR(255)          DEFAULT NULL COMMENT '软件包URL',
    `package_size`           BIGINT                DEFAULT NULL COMMENT '软件包大小（Byte）',
    `package_md5`            VARCHAR(255)          DEFAULT NULL COMMENT '软件包MD5',
    `package_desc`           VARCHAR(255)          DEFAULT NULL COMMENT '软件包说明',
    `package_type`           VARCHAR(20)  NOT NULL COMMENT '软件包类型',
    `package_source`         VARCHAR(20)  NOT NULL COMMENT '软件包来源',
    `base_software_pn`       VARCHAR(50)  NOT NULL COMMENT '基础软件零件号',
    `package_adaptive_level` SMALLINT     NOT NULL COMMENT '软件包适配级别：1-基础版本及以下，2-基础版本及以上，3-与基础版本一致',
    `adaptive_assembly_pn`   VARCHAR(50)  NOT NULL COMMENT '适配的总成零件号',
    `release_date`           TIMESTAMP    NOT NULL COMMENT '发布日期',
    `estimated_install_time` INT                   DEFAULT NULL COMMENT '预计升级时间（分钟）',
    `ota`                    TINYINT      NOT NULL COMMENT '是否是OTA包',
    `description`            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`              VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`              VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`            INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`              TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_device_code` (`device_code`),
    INDEX `idx_software_pn` (`software_pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件包信息表';
