-- Config word tables migrated from iov-cloud-ota-dota

CREATE TABLE `tb_config_word`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          VARCHAR(50)  NOT NULL COMMENT '配置字代码',
    `name`          VARCHAR(255) NOT NULL COMMENT '配置字名称',
    `device_code`   VARCHAR(20)  NOT NULL COMMENT '设备代码',
    `data_format`   VARCHAR(20)  NOT NULL COMMENT '数据格式',
    `byte_length`   SMALLINT     NOT NULL COMMENT '数据长度',
    `rw_capability` VARCHAR(50)  NOT NULL COMMENT '读写能力',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置字表';

CREATE TABLE `tb_config_word_profile`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_word_code`   VARCHAR(50)  NOT NULL COMMENT '配置字代码',
    `code`               VARCHAR(50)  NOT NULL COMMENT '配置文件代码',
    `name`               VARCHAR(255) NOT NULL COMMENT '配置文件名称',
    `read_type`          VARCHAR(20)  NOT NULL COMMENT '读取类型',
    `read_id`            VARCHAR(20)  NOT NULL COMMENT '读取ID',
    `write_type`         VARCHAR(20)           DEFAULT NULL COMMENT '写入类型',
    `write_id`           VARCHAR(20)           DEFAULT NULL COMMENT '写入ID',
    `session_type`       VARCHAR(20)           DEFAULT NULL COMMENT '会话类型',
    `security_level`     VARCHAR(20)           DEFAULT NULL COMMENT '安全策略',
    `timeout_ms`         INT                   DEFAULT NULL COMMENT '超时时间',
    `retry_times`        INT                   DEFAULT 0 COMMENT '重试次数',
    `software_pn`        VARCHAR(255)          DEFAULT NULL COMMENT '软件零件号范围',
    `software_build_ver` VARCHAR(255)          DEFAULT NULL COMMENT '软件内部版本范围',
    `description`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`          VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`        INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`          TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置字配置文件表';

CREATE TABLE `tb_config_word_field`
(
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_word_code`         VARCHAR(50)  NOT NULL COMMENT '配置字代码',
    `config_word_profile_code` VARCHAR(50)  NOT NULL COMMENT '配置字配置文件代码',
    `code`                     VARCHAR(50)  NOT NULL COMMENT '字段代码',
    `name`                     VARCHAR(255) NOT NULL COMMENT '字段名称',
    `byte_offset`              INT          NOT NULL COMMENT '字节偏移量',
    `bit_offset`               INT          NOT NULL COMMENT '位偏移量',
    `bit_length`               INT          NOT NULL COMMENT '位长度',
    `value_type`               VARCHAR(32)  NOT NULL COMMENT '值类型',
    `endianness`               VARCHAR(32)           DEFAULT NULL COMMENT '字节序',
    `description`              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`              INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置字字段表';
