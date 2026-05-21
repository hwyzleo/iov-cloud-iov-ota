CREATE TABLE IF NOT EXISTS `tb_activity`
(
    `id`                         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`                       VARCHAR(255) NOT NULL COMMENT '活动名称',
    `version`                    VARCHAR(255) NOT NULL COMMENT '活动版本',
    `upgrade_notice_article_id`  BIGINT                DEFAULT NULL COMMENT '升级须知文章ID',
    `activity_term_article_id`   BIGINT                DEFAULT NULL COMMENT '活动条款文章ID',
    `privacy_agreement_article_id` BIGINT              DEFAULT NULL COMMENT '隐私协议文章ID',
    `start_time`                 DATETIME              DEFAULT NULL COMMENT '活动开始时间',
    `end_time`                   DATETIME              DEFAULT NULL COMMENT '活动结束时间',
    `release_time`               DATETIME              DEFAULT NULL COMMENT '活动发布时间',
    `upgrade_purpose`            TEXT                  DEFAULT NULL COMMENT '升级目的',
    `upgrade_function`           TEXT                  DEFAULT NULL COMMENT '升级功能项',
    `statement`                  LONGTEXT              DEFAULT NULL COMMENT '活动说明',
    `state`                      SMALLINT     NOT NULL COMMENT '活动状态：1 待提交，2 待审核，3 已审核，4 未通过，5 已发布，6 已结束，7 已取消',
    `total_file_size`            BIGINT       NOT NULL DEFAULT 1 COMMENT '总文件大小（MB）',
    `baseline`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否基线活动',
    `baseline_code`              VARCHAR(255)          DEFAULT NULL COMMENT '基线代码',
    `description`                VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                  VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                  VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`                INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                  TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动表';

CREATE TABLE IF NOT EXISTS `tb_activity_compatible_pn`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`      BIGINT       NOT NULL COMMENT '活动ID',
    `compatible_pn_id` BIGINT      NOT NULL COMMENT '兼容零件号ID',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`       VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`     INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`       TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动兼容零件号关系表';

CREATE TABLE IF NOT EXISTS `tb_activity_fixed_config_word`
(
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`           BIGINT       NOT NULL COMMENT '升级活动ID',
    `fixed_config_word_id`  BIGINT       NOT NULL COMMENT '固定配置字ID',
    `description`           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`             VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`             VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`           INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`             TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动固定配置字表';

CREATE TABLE IF NOT EXISTS `tb_activity_software_build_version`
(
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`              BIGINT       NOT NULL COMMENT '活动ID',
    `software_build_version_id` BIGINT      NOT NULL COMMENT '软件内部版本ID',
    `critical`                 TINYINT      NOT NULL COMMENT '是否关键版本',
    `ota`                      TINYINT      NOT NULL COMMENT '是否支持OTA',
    `sort`                     SMALLINT     NOT NULL COMMENT '排序',
    `version_group`            SMALLINT     NOT NULL COMMENT '软件版本组',
    `force_upgrade`            TINYINT               DEFAULT 0 COMMENT '是否强制升级',
    `description`              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`              INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动软件内部版本关系表';

CREATE TABLE IF NOT EXISTS `tb_article`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(255) NOT NULL COMMENT '文章标题',
    `content`     LONGTEXT              DEFAULT NULL COMMENT '文章内容',
    `type`        SMALLINT     NOT NULL COMMENT '文章类型：1-活动条款，2-升级须知，3-隐私协议',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章表';

CREATE TABLE IF NOT EXISTS `tb_baseline`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`         VARCHAR(255) NOT NULL COMMENT '基线代码',
    `name`         VARCHAR(255)          DEFAULT NULL COMMENT '基线名称',
    `version`      VARCHAR(255)          DEFAULT NULL COMMENT '基线版本',
    `type`         SMALLINT     NOT NULL COMMENT '基线类型',
    `source`       SMALLINT     NOT NULL COMMENT '基线来源：1 TSP，2 XBOM',
    `series_code`  VARCHAR(255) NOT NULL COMMENT '车系代码',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`    BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`    BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`  INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='基线表';

CREATE TABLE IF NOT EXISTS `tb_ca_cert`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `seq_no`       VARCHAR(20)  NOT NULL COMMENT '流水号',
    `cert_entity`  LONGTEXT     NOT NULL COMMENT '证书实体',
    `cert_sn`      VARCHAR(255)          DEFAULT NULL COMMENT '证书序列号',
    `ctml_id`      VARCHAR(255)          DEFAULT NULL,
    `issuer`       VARCHAR(255)          DEFAULT NULL COMMENT '发行方',
    `p7b`          TEXT                  DEFAULT NULL,
    `public_key`   TEXT                  DEFAULT NULL COMMENT '公钥',
    `private_key`  TEXT                  DEFAULT NULL COMMENT '私钥',
    `expired_time` DATETIME              DEFAULT NULL COMMENT '到期时间',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`    BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`    BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`  INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='CA证书表';

CREATE TABLE IF NOT EXISTS `tb_compatible_software_pn`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ecu`                  VARCHAR(20)  NOT NULL COMMENT '零部件ECU',
    `software_pn`          VARCHAR(50)  NOT NULL COMMENT '软件零件号',
    `compatible_software_pn` VARCHAR(255) NOT NULL COMMENT '兼容软件零件号',
    `type`                 SMALLINT              DEFAULT NULL COMMENT '分类',
    `description`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`            VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`          INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`            TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='兼容软件零件号表';

CREATE TABLE IF NOT EXISTS `tb_ota_ecu`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `series_code`       VARCHAR(255) NOT NULL COMMENT '车系代码',
    `code`             VARCHAR(255) NOT NULL COMMENT 'ECU代码',
    `abbr_code`         VARCHAR(255) NOT NULL COMMENT 'ECU简称代码',
    `name`             VARCHAR(255) NOT NULL COMMENT 'ECU名称',
    `partition_type`   SMALLINT     NOT NULL COMMENT '分区类型：1 AB分区，2 单分区',
    `safety_part`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否解闭锁安全件',
    `failure_strategy` TINYINT      NOT NULL DEFAULT 0 COMMENT '升级失败策略：1 可正常行驶，0 不可正常行驶',
    `vehicle_impact`   TINYINT      NOT NULL DEFAULT 0 COMMENT '用车影响：1 是，0 否',
    `install_time`      INT                   DEFAULT NULL COMMENT '预计安装时长（分钟）',
    `sort`             INT          NOT NULL DEFAULT 0 COMMENT '刷写顺序',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`        BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`        BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`      INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`            TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='可升级ECU表';

CREATE TABLE IF NOT EXISTS `tb_part`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sn`            VARCHAR(255) NOT NULL COMMENT '序列号',
    `no`            VARCHAR(255)          DEFAULT NULL COMMENT '零部件编号',
    `ecu`           VARCHAR(20)  NOT NULL COMMENT '零部件ECU',
    `config_word`   VARCHAR(255)          DEFAULT NULL COMMENT '配置字',
    `supplier_code` VARCHAR(255)          DEFAULT NULL COMMENT '供应商编码',
    `hardware_ver`  VARCHAR(255)          DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)          DEFAULT NULL COMMENT '软件版本号',
    `hardware_no`   VARCHAR(255)          DEFAULT NULL COMMENT '硬件零件号',
    `software_no`   VARCHAR(255)          DEFAULT NULL COMMENT '软件零件号',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `sn` (`sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='零部件信息表';

CREATE TABLE IF NOT EXISTS `tb_part_log`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sn`            VARCHAR(255) NOT NULL COMMENT '序列号',
    `config_word`   VARCHAR(255)          DEFAULT NULL COMMENT '配置字',
    `hardware_ver`  VARCHAR(255)          DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)          DEFAULT NULL COMMENT '软件版本号',
    `hardware_no`   VARCHAR(255)          DEFAULT NULL COMMENT '硬件零件号',
    `software_no`   VARCHAR(255)          DEFAULT NULL COMMENT '软件零件号',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_sn` (`sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='零部件信息变更日志表';

CREATE TABLE IF NOT EXISTS `tb_part_version`
(
    `id`                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `series_code`                 VARCHAR(255)  NOT NULL COMMENT '车系代码',
    `ecu_code`                    VARCHAR(255)  NOT NULL COMMENT 'ECU代码',
    `software_no_code`            VARCHAR(255)  NOT NULL COMMENT '软件零件号代码',
    `software_no_version_range`   VARCHAR(2000)          DEFAULT NULL COMMENT '软件零件号版本范围',
    `description`                 VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`                   BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`                 DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`                   BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`                 DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`                 INT           NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`                       TINYINT       NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='零部件版本表';

CREATE TABLE IF NOT EXISTS `tb_setting`
(
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                  VARCHAR(20)   NOT NULL COMMENT '车架号',
    `mode`                 SMALLINT      NOT NULL COMMENT '升级模式：1 自动，2 非自动',
    `version_history`      LONGTEXT               DEFAULT NULL COMMENT '版本历史',
    `upgrade_time`         DATETIME               DEFAULT NULL COMMENT '最后升级时间',
    `upgrade_version`      VARCHAR(255)           DEFAULT NULL COMMENT '最后升级版本',
    `upgrade_activity_id`  BIGINT                 DEFAULT NULL COMMENT '最后升级活动ID',
    `upgrade_task_id`      BIGINT                 DEFAULT NULL COMMENT '最后升级任务ID',
    `upgrade_config_word`  VARCHAR(2000)          DEFAULT NULL COMMENT '最后升级配置字',
    `baseline_code`        VARCHAR(255)           DEFAULT NULL COMMENT '基线代码',
    `baseline_alignment`   TINYINT                DEFAULT NULL COMMENT '基线是否对齐',
    `baseline_code_history` DATETIME              DEFAULT NULL COMMENT '历史基线代码',
    `maintain`             TINYINT                DEFAULT NULL COMMENT '是否保养',
    `description`          VARCHAR(255)           DEFAULT NULL COMMENT '描述',
    `create_by`           BIGINT                 DEFAULT NULL COMMENT '创建者',
    `create_time`         DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`           BIGINT                 DEFAULT NULL COMMENT '更新者',
    `modify_time`         DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`         INT           NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`               TINYINT       NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆升级设置表';

CREATE TABLE IF NOT EXISTS `tb_fota_software_package`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `series_code`         VARCHAR(255) NOT NULL COMMENT '车系代码',
    `software_no`         VARCHAR(255) NOT NULL COMMENT '软件零件号',
    `ecu_code`            VARCHAR(255)          DEFAULT NULL COMMENT 'ECU代码',
    `file_name`           VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件名',
    `file_code`           VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件代码',
    `file_size`           BIGINT                DEFAULT NULL COMMENT '软件包文件大小（Byte）',
    `file_md5`            VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件MD5',
    `install_time`        INT                   DEFAULT NULL COMMENT '预计安装时长（分钟）',
    `type`                SMALLINT     NOT NULL COMMENT '软件包类型：0 全量，1 差分',
    `source`              SMALLINT     NOT NULL COMMENT '软件包来源：1 TSP，2 XBOM',
    `xbom_package_code`   VARCHAR(255)          DEFAULT NULL COMMENT 'XBOM软件包编码',
    `base_software_no`    VARCHAR(255)          DEFAULT NULL COMMENT '基础软件零件号',
    `base_software_version` VARCHAR(255)        DEFAULT NULL COMMENT '基础软件版本',
    `adaption`            SMALLINT     NOT NULL COMMENT '适配级别：1 基础版本及以下，2 基础版本及以上，3 与基础版本一致',
    `sign`                TEXT                  DEFAULT NULL COMMENT '软件包签名',
    `sign_expired`        DATE                  DEFAULT NULL COMMENT '软件包签名有效期',
    `allow_modify`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许修改',
    `allow_delete`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许删除',
    `description`         VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`           BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`         DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`           BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`         DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`         INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`               TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='FOTA软件升级包表';

CREATE TABLE IF NOT EXISTS `tb_software_package_his`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name`     VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件名',
    `file_code`     VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件代码',
    `file_size`     BIGINT                DEFAULT NULL COMMENT '软件包文件大小（Byte）',
    `file_md5`      VARCHAR(255)          DEFAULT NULL COMMENT '软件包文件MD5',
    `sign`          TEXT                  DEFAULT NULL COMMENT '软件包签名',
    `sign_expired`  DATE                  DEFAULT NULL COMMENT '软件包签名有效期',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`     BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`     BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`   INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件升级包历史表';

CREATE TABLE IF NOT EXISTS `tb_software_version`
(
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `series_code`          VARCHAR(255)  NOT NULL COMMENT '车系代码',
    `software_no`          VARCHAR(255)  NOT NULL COMMENT '软件零件号',
    `ecu_code`             VARCHAR(255)           DEFAULT NULL COMMENT 'ECU代码',
    `software_version`     VARCHAR(255)  NOT NULL COMMENT '软件版本',
    `compitable_assembly`  VARCHAR(2000)          DEFAULT NULL COMMENT '兼容的总成/硬件',
    `release_date`        DATE                   DEFAULT NULL COMMENT '发布日期',
    `test_file_name`       VARCHAR(255)           DEFAULT NULL COMMENT '测试报告文件名',
    `test_file_code`       VARCHAR(255)           DEFAULT NULL COMMENT '测试报告文件代码',
    `source`               SMALLINT      NOT NULL COMMENT '软件来源：1 TSP，2 XBOM',
    `config_word_changed`  TINYINT       NOT NULL DEFAULT 0 COMMENT '配置字是否变更',
    `allow_modify`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许修改',
    `allow_delete`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许删除',
    `description`          VARCHAR(255)           DEFAULT NULL COMMENT '描述',
    `create_by`            BIGINT                 DEFAULT NULL COMMENT '创建者',
    `create_time`          DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`            BIGINT                 DEFAULT NULL COMMENT '更新者',
    `modify_time`          DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`          INT           NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`                TINYINT       NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件版本表';

CREATE TABLE IF NOT EXISTS `tb_task`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(255) NOT NULL COMMENT '任务名称',
    `type`             SMALLINT     NOT NULL DEFAULT 1 COMMENT '任务类型：1=普通任务，2=快速任务',
    `phase`            SMALLINT     NOT NULL DEFAULT 1 COMMENT '任务阶段：1=验证，2=灰度，3=发布',
    `activity_id`      BIGINT       NOT NULL COMMENT '升级活动ID',
    `target`           VARCHAR(255) NOT NULL COMMENT '升级对象，普通任务时为文件代码，快速任务时为VIN',
    `start_time`       DATETIME              DEFAULT NULL COMMENT '任务开始时间',
    `end_time`         DATETIME              DEFAULT NULL COMMENT '任务结束时间',
    `release_time`     DATETIME              DEFAULT NULL COMMENT '任务发布时间',
    `notice_type`      VARCHAR(255)          DEFAULT NULL COMMENT '通知类型（多选）：1 手机',
    `upgrade_mode`     SMALLINT              DEFAULT NULL COMMENT '升级模式：1=普通，2=强制，3=预约静默，4=远程静默，5=工厂',
    `upgrade_mode_arg` VARCHAR(255)          DEFAULT NULL COMMENT '升级模式参数',
    `state`            SMALLINT     NOT NULL COMMENT '任务状态：1=待提交，2=待审核，3=已审核，4=未通过，5=已发布，6=已暂停，7=已结束，8=已取消',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`        VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`      INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`        TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务表';

CREATE TABLE IF NOT EXISTS `tb_task_restriction`
(
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`               BIGINT       NOT NULL COMMENT '升级任务ID',
    `restriction_type`      VARCHAR(255) NOT NULL COMMENT '限制条件类型',
    `restriction_expression` VARCHAR(255) NOT NULL COMMENT '限制条件表达式',
    `description`           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`             VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`             VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`           INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`             TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task` (`task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务限制条件表';

CREATE TABLE IF NOT EXISTS `tb_task_strategy`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`             BIGINT       NOT NULL COMMENT '升级任务ID',
    `strategy_type`       VARCHAR(255) NOT NULL COMMENT '策略类型',
    `strategy_expression` VARCHAR(255) NOT NULL COMMENT '限制条件表达式',
    `description`         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`           VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`           VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`         INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`           TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task` (`task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务策略表';

CREATE TABLE IF NOT EXISTS `tb_task_target`
(
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`              VARCHAR(255)  NOT NULL COMMENT '车架号',
    `series_code`      VARCHAR(255)           DEFAULT NULL COMMENT '车系代码',
    `activity_id`      BIGINT        NOT NULL COMMENT '升级活动ID',
    `task_id`          BIGINT        NOT NULL COMMENT '升级任务ID',
    `upgrade_state`    SMALLINT      NOT NULL DEFAULT 0 COMMENT '升级状态：0 未开始，1 开始下载，2 继续下载，3 结束下载，4 预约升级，5 自动升级，6 检测，7 开始安装，8 结束安装，9 开始回滚，10 结束回滚，11 升级立即重启，12 升级用户重启，13 回滚立即重启，14 回滚用户重启，15 写配置字，16 回滚配置字',
    `upgrade_result`   TINYINT                DEFAULT NULL COMMENT '升级结果：1 成功，0 失败',
    `upgrade_ecu_info` TEXT                   DEFAULT NULL COMMENT '升级ECU版本信息',
    `error_code`       VARCHAR(255)           DEFAULT NULL COMMENT '错误码',
    `error_msg`        VARCHAR(255)           DEFAULT NULL COMMENT '错误描述',
    `description`     VARCHAR(255)           DEFAULT NULL COMMENT '描述',
    `create_by`       BIGINT                 DEFAULT NULL COMMENT '创建者',
    `create_time`      DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`       BIGINT                 DEFAULT NULL COMMENT '更新者',
    `modify_time`     DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`     INT           NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`           TINYINT       NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务目标表';

CREATE TABLE IF NOT EXISTS `tb_task_target_log`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_id`   BIGINT       NOT NULL COMMENT '任务目标ID',
    `file_code`   VARCHAR(255) NOT NULL COMMENT '日志文件代码',
    `file_name`   VARCHAR(255) NOT NULL COMMENT '日志文件名称',
    `upload_time` DATETIME              DEFAULT NULL COMMENT '上传时间',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`   BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version` INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务目标日志表';

CREATE TABLE IF NOT EXISTS `tb_task_target_mp_process`
(
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                    VARCHAR(20)  NOT NULL COMMENT '车架号',
    `activity_id`            BIGINT       NOT NULL COMMENT '活动ID',
    `task_id`               BIGINT       NOT NULL COMMENT '任务ID',
    `progress`              SMALLINT              DEFAULT NULL COMMENT '升级进度',
    `operation_type`        SMALLINT              DEFAULT NULL COMMENT '操作类型',
    `check_result`          VARCHAR(255)          DEFAULT NULL COMMENT '检测结果',
    `operation_result`      TINYINT               DEFAULT NULL COMMENT '操作结果：1 成功，0 失败',
    `remote_operation_type` SMALLINT              DEFAULT NULL COMMENT '远程操作类型：1 预约，2 更新预约，3 取消预约，4 立即升级',
    `remote_operation_terminal` SMALLINT          DEFAULT NULL COMMENT '远程操作终端：1 手机，2 车机',
    `remote_operation_state` SMALLINT             DEFAULT NULL COMMENT '远程操作状态：1 执行中，2 成功，3 失败',
    `appointment_time`      DATETIME              DEFAULT NULL COMMENT '预约时间',
    `package_info`          VARCHAR(255)          DEFAULT NULL COMMENT '安装包信息',
    `display_version`       VARCHAR(255)          DEFAULT NULL COMMENT '显示版本',
    `vehicle_available`     TINYINT               DEFAULT NULL COMMENT '车辆是否可用',
    `rollback_strategy`     VARCHAR(255)          DEFAULT NULL COMMENT '回滚策略',
    `description`           VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`             BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`           DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`             BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`           DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`           INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`                 TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务目标手机升级进度表';

CREATE TABLE IF NOT EXISTS `tb_task_target_process`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_id`       BIGINT       NOT NULL COMMENT '任务目标ID',
    `part_type`       VARCHAR(255)          DEFAULT NULL COMMENT '零部件类型',
    `part_sn`         BIGINT       NOT NULL COMMENT '零部件序列号',
    `task_id`         BIGINT       NOT NULL COMMENT '升级任务ID',
    `operation_type`  SMALLINT     NOT NULL COMMENT '1 开始下载，2 继续下载，3 结束下载，4 预约升级，5 自动升级，6 检测，7 开始安装，8 结束安装，9 开始回滚，10 结束回滚，11 升级立即重启，12 升级用户重启，13 回滚立即重启，14 回滚用户重启，15 写配置字，16 回滚配置字',
    `operation_time`  DATETIME              DEFAULT NULL COMMENT '操作时间',
    `operation_result` TINYINT              DEFAULT NULL COMMENT '操作结果：1 成功，0 失败',
    `retry_times`     SMALLINT              DEFAULT NULL COMMENT '重试次数',
    `ext_attr`        VARCHAR(255)          DEFAULT NULL COMMENT '扩展属性',
    `error_code`      VARCHAR(255)          DEFAULT NULL COMMENT '错误码',
    `error_msg`       VARCHAR(255)          DEFAULT NULL COMMENT '错误描述',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `create_by`       BIGINT                DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by`       BIGINT                DEFAULT NULL COMMENT '更新者',
    `modify_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `row_version`     INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `valid`           TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务目标过程表';

CREATE TABLE IF NOT EXISTS `tb_task_vehicle`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`  BIGINT       NOT NULL COMMENT '升级活动ID',
    `task_id`      BIGINT       NOT NULL COMMENT '升级任务ID',
    `vin`          VARCHAR(20)  NOT NULL COMMENT '车架号',
    `state`        SMALLINT     NOT NULL COMMENT '车辆任务状态：0=等待下载，1=开始下载，3=继续下载，5=结束下载，7=预约升级，9=自动升级，10=安装检测，11=开始安装，15=结束安装，17=开始回滚，19=结束回滚，21=升级立即重启，22=升级用户重启，23=回滚立即重启，24=回滚用户重启，25=写配置字，26=回滚配置字，90=升级失败，91=升级超时',
    `result_code`  VARCHAR(20)           DEFAULT NULL COMMENT '结果代码',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`    VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`  INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`    TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activity` (`activity_id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务车辆表';

CREATE TABLE IF NOT EXISTS `tb_task_vehicle_detail`
(
    `id`           BIGINT       NOT NULL COMMENT '升级任务车辆主键',
    `fota_info`    TEXT                  DEFAULT NULL COMMENT '升级信息',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`    VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`  INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`    TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务车辆详情表';

CREATE TABLE IF NOT EXISTS `tb_task_vehicle_process`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '升级任务车辆主键',
    `task_id`          BIGINT       NOT NULL COMMENT '升级任务ID',
    `vin`              VARCHAR(20)  NOT NULL COMMENT '车架号',
    `device`           VARCHAR(20)  NOT NULL COMMENT '升级设备',
    `device_sn`        VARCHAR(20)           DEFAULT NULL COMMENT '设备序列号',
    `operation`        SMALLINT     NOT NULL COMMENT '执行操作',
    `operation_time`   TIMESTAMP    NOT NULL COMMENT '操作时间',
    `operation_result` SMALLINT              DEFAULT NULL COMMENT '操作结果',
    `retry_count`      SMALLINT              DEFAULT NULL COMMENT '重试次数',
    `error_code`       SMALLINT              DEFAULT NULL COMMENT '异常代码',
    `error_msg`        VARCHAR(255)          DEFAULT NULL COMMENT '异常消息',
    `extra`            VARCHAR(255)          DEFAULT NULL COMMENT '扩展内容',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`        VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`      INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`        TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级任务车辆升级过程表';

CREATE TABLE IF NOT EXISTS `tb_veh_part`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`          VARCHAR(20)  NOT NULL COMMENT '车架号',
    `ecu`          VARCHAR(20)  NOT NULL COMMENT '零部件ECU',
    `part_sn`      VARCHAR(255) NOT NULL COMMENT '零部件序列号',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`    VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`  INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`    TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零部件关系表';

CREATE TABLE IF NOT EXISTS `tb_veh_part_log`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`          VARCHAR(20)  NOT NULL COMMENT '车架号',
    `ecu`          VARCHAR(20)  NOT NULL COMMENT '零部件ECU',
    `part_sn`      VARCHAR(255) NOT NULL COMMENT '零部件序列号',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`    VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`  INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`    TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零部件关系变更日志表';

CREATE TABLE IF NOT EXISTS `tb_veh_status`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                 VARCHAR(20)  NOT NULL COMMENT '车架号',
    `report_time`         TIMESTAMP             DEFAULT NULL COMMENT '最后上报时间',
    `baseline_code`       VARCHAR(255)          DEFAULT NULL COMMENT '最后基线代码',
    `baseline_alignment`  TINYINT               DEFAULT NULL COMMENT '最后基线是否对齐',
    `device_info`         TEXT                  DEFAULT NULL COMMENT '最后设备信息',
    `activity_id`         BIGINT                DEFAULT NULL COMMENT '最后升级活动ID',
    `task_id`             BIGINT                DEFAULT NULL COMMENT '最后升级任务ID',
    `config_word`         VARCHAR(2000)         DEFAULT NULL COMMENT '最后升级配置字',
    `master_version`      VARCHAR(255)          DEFAULT NULL COMMENT '最后OTA Master版本',
    `description`         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`           VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`           VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`         INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`           TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆状态表';

CREATE TABLE IF NOT EXISTS `tb_vehicle_setting`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                VARCHAR(20)  NOT NULL COMMENT '车架号',
    `baseline_code`      VARCHAR(255)          DEFAULT NULL COMMENT '基线代码',
    `baseline_alignment` TINYINT               DEFAULT NULL COMMENT '基线是否对齐',
    `description`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`          VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`        INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`          TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆设置表';

CREATE TABLE IF NOT EXISTS `tr_activity_software_version`
(
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`         BIGINT NOT NULL COMMENT '升级活动ID',
    `software_version_id` BIGINT NOT NULL COMMENT '软件版本ID',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动与软件版本关联表';

CREATE TABLE IF NOT EXISTS `tr_activity_software_version_group`
(
    `id`                  BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`         BIGINT   NOT NULL COMMENT '活动ID',
    `software_version_id` BIGINT   NOT NULL COMMENT '软件版本ID',
    `sort`                SMALLINT NOT NULL COMMENT '排序',
    `group`               SMALLINT NOT NULL COMMENT '软件版本组',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='活动与软件版本组关联表';

CREATE TABLE IF NOT EXISTS `tr_baseline_software_version`
(
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `baseline_id`         BIGINT NOT NULL COMMENT '基线ID',
    `software_version_id` BIGINT NOT NULL COMMENT '软件版本ID',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='基线与软件版本关联表';

CREATE TABLE IF NOT EXISTS `tr_software_version_dependency`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version_id`           BIGINT       NOT NULL COMMENT '软件版本ID',
    `dependency_version_id` BIGINT      NOT NULL COMMENT '依赖软件版本ID',
    `adaption`             SMALLINT     NOT NULL COMMENT '适配级别：1 基础版本及以下，2 基础版本及以上，3 与基础版本一致',
    `description`          VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件版本与其他软件版本依赖关系表';

CREATE TABLE IF NOT EXISTS `tr_software_version_package`
(
    `id`            BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `package_id`    BIGINT  NOT NULL COMMENT '软件升级包ID',
    `version_id`    BIGINT  NOT NULL COMMENT '软件版本ID',
    `allow_modify`  TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许修改',
    `allow_delete`  TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件版本与软件包关系表';