DROP TABLE IF EXISTS `db_pota`.`tb_software_package`;
CREATE TABLE `db_pota`.`tb_software_package`
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

DROP TABLE IF EXISTS `db_pota`.`tb_software_build_version`;
CREATE TABLE `db_pota`.`tb_software_build_version`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_code`          VARCHAR(20)  NOT NULL COMMENT '设备编码',
    `software_pn`          VARCHAR(50)  NOT NULL COMMENT '软件零件号',
    `software_build_ver`   VARCHAR(255) NOT NULL COMMENT '软件内部版本',
    `software_report`      VARCHAR(255)          DEFAULT NULL COMMENT '软件测试报告',
    `software_desc`        VARCHAR(255)          DEFAULT NULL COMMENT '软件说明',
    `software_source`      VARCHAR(20)  NOT NULL COMMENT '软件来源',
    `adaptive_assembly_pn` VARCHAR(255) NOT NULL COMMENT '适配的总成零件号',
    `adaptive_software_pn` VARCHAR(255)          DEFAULT NULL COMMENT '适配的软件零件号',
    `release_date`         TIMESTAMP    NOT NULL COMMENT '发布日期',
    `description`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`            VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`          INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`            TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_software_pn` (`software_pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件内部版本信息表';

DROP TABLE IF EXISTS `db_pota`.`tb_software_build_version_package`;
CREATE TABLE `db_pota`.`tb_software_build_version_package`
(
    `id`                        BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `software_build_version_id` BIGINT    NOT NULL COMMENT '软件零件版本ID',
    `software_package_id`       BIGINT    NOT NULL COMMENT '软件包ID',
    `sort`                      INT       NOT NULL COMMENT '排序',
    `description`               VARCHAR(255)       DEFAULT NULL COMMENT '备注',
    `create_time`               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                 VARCHAR(64)        DEFAULT NULL COMMENT '创建者',
    `modify_time`               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                 VARCHAR(64)        DEFAULT NULL COMMENT '修改者',
    `row_version`               INT                DEFAULT 1 COMMENT '记录版本',
    `row_valid`                 TINYINT            DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_software_build_version` (`software_build_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件内部版本软件包关系表';

DROP TABLE IF EXISTS `db_pota`.`tb_software_build_version_dependency`;
CREATE TABLE `db_pota`.`tb_software_build_version_dependency`
(
    `id`                                   BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `software_build_version_id`            BIGINT    NOT NULL COMMENT '软件内部版本ID',
    `dependency_software_build_version_id` BIGINT    NOT NULL COMMENT '依赖软件内部版本ID',
    `adaptive_level`                       SMALLINT  NOT NULL COMMENT '适配级别：1-版本及以下，2-版本及以上，3-与版本一致',
    `sort`                                 INT       NOT NULL COMMENT '排序',
    `description`                          VARCHAR(255)       DEFAULT NULL COMMENT '备注',
    `create_time`                          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                            VARCHAR(64)        DEFAULT NULL COMMENT '创建者',
    `modify_time`                          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                            VARCHAR(64)        DEFAULT NULL COMMENT '修改者',
    `row_version`                          INT                DEFAULT 1 COMMENT '记录版本',
    `row_valid`                            TINYINT            DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_software_build_version` (`software_build_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件内部版本依赖关系表';

DROP TABLE IF EXISTS `db_pota`.`tb_compatible_pn`;
CREATE TABLE `db_pota`.`tb_compatible_pn`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`          SMALLINT     NOT NULL COMMENT '类型：1-软件零件号，2-硬件零件号',
    `device_code`   VARCHAR(20)  NOT NULL COMMENT '设备代码',
    `pn`            VARCHAR(50)  NOT NULL COMMENT '零件号',
    `compatible_pn` VARCHAR(255) NOT NULL COMMENT '兼容零件号',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '兼容零件号表';

DROP TABLE IF EXISTS `db_pota`.`tb_data_sync_record`;
CREATE TABLE `db_pota`.`tb_data_sync_record`
(
    `id`          BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `source`      SMALLINT  NOT NULL COMMENT '同步来源',
    `type`        SMALLINT  NOT NULL COMMENT '同步类型',
    `code`        VARCHAR(255)       DEFAULT NULL COMMENT '代码',
    `data`        LONGTEXT           DEFAULT NULL COMMENT '同步数据',
    `state`       SMALLINT  NOT NULL COMMENT '同步状态：0-开始同步，1-同步成功，-1-同步失败',
    `description` VARCHAR(255)       DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)        DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)        DEFAULT NULL COMMENT '修改者',
    `row_version` INT                DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT            DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '数据同步记录表';

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