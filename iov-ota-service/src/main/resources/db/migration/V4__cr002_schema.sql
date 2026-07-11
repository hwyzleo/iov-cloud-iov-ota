-- DSN-CR-002: CR-002 Schema Migration
-- 1. Drop legacy POTA tables not used by new design
-- 2. Recreate tb_baseline as MDM projection
-- 3. Create projection tables (baseline_item, swin_definition, swin_managed_system)
-- 4. ALTER tb_article for multi-language/version/type
-- 5. Create 13 CR-002 new tables

-- 1. Drop legacy POTA tables (not referenced by any PO)
DROP TABLE IF EXISTS `tr_software_version_package`;
DROP TABLE IF EXISTS `tr_software_version_dependency`;
DROP TABLE IF EXISTS `tr_baseline_software_version`;
DROP TABLE IF EXISTS `tr_activity_software_version_group`;
DROP TABLE IF EXISTS `tr_activity_software_version`;
DROP TABLE IF EXISTS `tb_vehicle_setting`;
DROP TABLE IF EXISTS `tb_veh_part_log`;
DROP TABLE IF EXISTS `tb_veh_part`;
DROP TABLE IF EXISTS `tb_software_version`;
DROP TABLE IF EXISTS `tb_software_package_his`;
DROP TABLE IF EXISTS `tb_setting`;
DROP TABLE IF EXISTS `tb_part_version`;
DROP TABLE IF EXISTS `tb_part_log`;
DROP TABLE IF EXISTS `tb_part`;
DROP TABLE IF EXISTS `tb_ota_ecu`;
DROP TABLE IF EXISTS `tb_fota_software_package`;
DROP TABLE IF EXISTS `tb_compatible_software_pn`;
DROP TABLE IF EXISTS `tb_ca_cert`;
DROP TABLE IF EXISTS `tb_task_target_mp_process`;
DROP TABLE IF EXISTS `tb_task_target_process`;
DROP TABLE IF EXISTS `tb_task_target_log`;
DROP TABLE IF EXISTS `tb_task_target`;

-- 2. Drop old tb_baseline (legacy POTA schema) and recreate as MDM projection
DROP TABLE IF EXISTS `tb_baseline`;

CREATE TABLE IF NOT EXISTS `tb_baseline`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `baseline_code`    VARCHAR(255) NOT NULL COMMENT '基线代码（MDM SoftwareBaseline.code）',
    `name`             VARCHAR(255)          DEFAULT NULL COMMENT '基线名称',
    `anchor_type`      VARCHAR(32)  NOT NULL COMMENT '锚定类型：CONFIGURATION / VARIANT',
    `anchor_code`      VARCHAR(255) NOT NULL COMMENT '锚定代码',
    `baseline_version` VARCHAR(255)          DEFAULT NULL COMMENT '基线版本',
    `baseline_status`  VARCHAR(32)  NOT NULL COMMENT '基线状态：仅消费 RELEASED',
    `source`           VARCHAR(32)  NOT NULL DEFAULT 'MDM' COMMENT '数据来源',
    `sync_time`        DATETIME              DEFAULT NULL COMMENT '最后同步时间',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`        VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`      INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`        TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_baseline_code` (`baseline_code`),
    INDEX `idx_anchor` (`anchor_type`, `anchor_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='配置软件应装基线投影表（MDM Material SoftwareBaseline）';

-- 3. Create tb_baseline_item (MDM projection)
CREATE TABLE IF NOT EXISTS `tb_baseline_item`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `baseline_code`     VARCHAR(255) NOT NULL COMMENT '基线代码',
    `part_code`         VARCHAR(64)  NOT NULL COMMENT '软件零件号',
    `vehicle_node_code` VARCHAR(64)           DEFAULT NULL COMMENT '车载节点代码',
    `remark`            VARCHAR(512)          DEFAULT NULL COMMENT '备注',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_baseline_code` (`baseline_code`),
    INDEX `idx_part_code` (`part_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='基线软件零件号应装清单投影表';

-- 4. Create tb_swin_definition (MDM EEAD projection)
CREATE TABLE IF NOT EXISTS `tb_swin_definition`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `swin_code`     VARCHAR(64)  NOT NULL COMMENT 'SWIN代码',
    `scheme_code`   VARCHAR(64)  NOT NULL COMMENT '编码方案代码',
    `type_ref_type` VARCHAR(32)           DEFAULT NULL COMMENT '引用类型：VARIANT / MODEL',
    `type_ref_code` VARCHAR(255)          DEFAULT NULL COMMENT '引用代码',
    `name`          VARCHAR(255)          DEFAULT NULL COMMENT '名称',
    `status`        VARCHAR(32)  NOT NULL COMMENT '状态：仅消费 ACTIVE',
    `sync_time`     DATETIME              DEFAULT NULL COMMENT '最后同步时间',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_swin_code` (`swin_code`),
    INDEX `idx_scheme_code` (`scheme_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SWIN定义投影表（MDM EEAD）';

-- 5. Create tb_swin_managed_system (MDM EEAD projection)
CREATE TABLE IF NOT EXISTS `tb_swin_managed_system`
(
    `id`                         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `swin_code`                  VARCHAR(64)  NOT NULL COMMENT 'SWIN代码',
    `vehicle_node_code`          VARCHAR(64)  NOT NULL COMMENT '车载节点代码',
    `is_type_approval_relevant`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否与型式批准相关：1-是，0-否',
    `approved_software_baseline` VARCHAR(128)          DEFAULT NULL COMMENT '已批准的软件基线代码（软引用）',
    `description`                VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                  VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                  VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`                INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                  TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_swin_code` (`swin_code`),
    INDEX `idx_vehicle_node_code` (`vehicle_node_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SWIN受管系统清单投影表（MDM EEAD）';

-- 6. ALTER tb_article for multi-language / version / type
ALTER TABLE `tb_article`
    ADD COLUMN `lang`         VARCHAR(16)  NOT NULL DEFAULT 'zh-CN' COMMENT '语言' AFTER `type`,
    ADD COLUMN `version`      VARCHAR(64)           DEFAULT NULL COMMENT '版本' AFTER `lang`,
    ADD COLUMN `article_type` VARCHAR(32)  NOT NULL DEFAULT 'NOTICE' COMMENT '文章类型：RELEASE_NOTE / NOTICE / TERMS / PRIVACY' AFTER `version`,
    ADD COLUMN `status`       VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT / PUBLISHED' AFTER `article_type`;

-- 7.1 tb_upgrade_package (US-018~022)
CREATE TABLE IF NOT EXISTS `tb_upgrade_package`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`               BIGINT       NOT NULL COMMENT '升级活动ID',
    `software_build_version_id` BIGINT       NOT NULL COMMENT '软件内部版本ID',
    `package_type`              VARCHAR(20)  NOT NULL COMMENT '包类型：FULL / DELTA',
    `base_software_pn`          VARCHAR(64)           DEFAULT NULL COMMENT '基础软件零件号（DELTA包）',
    `base_software_ver`         VARCHAR(255)          DEFAULT NULL COMMENT '基础软件版本（DELTA包）',
    `target_software_pn`        VARCHAR(64)  NOT NULL COMMENT '目标软件零件号',
    `target_software_ver`       VARCHAR(255) NOT NULL COMMENT '目标软件版本',
    `package_url`               VARCHAR(512)          DEFAULT NULL COMMENT '升级包URL（对象存储）',
    `package_size`              BIGINT                DEFAULT NULL COMMENT '升级包大小（Byte）',
    `package_md5`               VARCHAR(255)          DEFAULT NULL COMMENT '升级包MD5',
    `package_sha256`            VARCHAR(255)          DEFAULT NULL COMMENT '升级包SHA256',
    `sign_ref`                  VARCHAR(255)          DEFAULT NULL COMMENT '签名密钥引用（KMS keyName）',
    `encrypt_ref`               VARCHAR(255)          DEFAULT NULL COMMENT '加密密钥引用（KMS keyName）',
    `build_state`               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '构建状态：PENDING / RUNNING / SUCCESS / FAIL',
    `test_state`                VARCHAR(20)  NOT NULL DEFAULT 'UNTESTED' COMMENT '测试状态：UNTESTED / TESTING / PASSED / FAILED',
    `ota`                       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否OTA包',
    `description`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`               INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                 TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`),
    INDEX `idx_sbv` (`software_build_version_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='升级包表';

-- 7.2 tb_upgrade_package_build (US-018)
CREATE TABLE IF NOT EXISTS `tb_upgrade_package_build`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `upgrade_package_id`  BIGINT       NOT NULL COMMENT '升级包ID',
    `build_type`          VARCHAR(20)  NOT NULL COMMENT '构建类型：AUTO / MANUAL',
    `state`               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '构建状态：PENDING / RUNNING / SUCCESS / FAIL',
    `error_msg`           VARCHAR(512)          DEFAULT NULL COMMENT '错误信息',
    `start_time`          DATETIME              DEFAULT NULL COMMENT '开始时间',
    `end_time`            DATETIME              DEFAULT NULL COMMENT '结束时间',
    `description`         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`           VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`           VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`         INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`           TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_upgrade_package` (`upgrade_package_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='升级包构建任务表';

-- 7.3 tb_activity_target_version (US-023/024)
CREATE TABLE IF NOT EXISTS `tb_activity_target_version`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`               BIGINT       NOT NULL COMMENT '升级活动ID',
    `baseline_code`             VARCHAR(255)          DEFAULT NULL COMMENT '基线代码',
    `vehicle_node_code`         VARCHAR(64)  NOT NULL COMMENT '车载节点代码',
    `part_code`                 VARCHAR(64)  NOT NULL COMMENT '软件零件号',
    `target_software_build_ver` VARCHAR(255) NOT NULL COMMENT '目标软件内部版本',
    `description`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`               INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                 TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='升级目标版本表';

-- 7.4 tb_activity_install_order (US-027)
CREATE TABLE IF NOT EXISTS `tb_activity_install_order`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`       BIGINT       NOT NULL COMMENT '升级活动ID',
    `vehicle_node_code` VARCHAR(64)  NOT NULL COMMENT '车载节点代码',
    `seq_no`            INT          NOT NULL COMMENT '顺序号',
    `parallel_group`    INT                   DEFAULT NULL COMMENT '并行组号（同组并行执行）',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='ECU升级顺序表';

-- 7.5 tb_activity_dependency_group (US-027)
CREATE TABLE IF NOT EXISTS `tb_activity_dependency_group`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`       BIGINT       NOT NULL COMMENT '升级活动ID',
    `group_code`        VARCHAR(64)  NOT NULL COMMENT '关联组代码',
    `member_node_code`  VARCHAR(64)  NOT NULL COMMENT '组成员车载节点代码',
    `rollback_together` TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否同升同降：1-是，0-否',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`),
    INDEX `idx_group_code` (`group_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='依赖关联组表（同升同降）';

-- 7.6 tb_task_batch (US-031)
CREATE TABLE IF NOT EXISTS `tb_task_batch`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`     BIGINT       NOT NULL COMMENT '升级任务ID',
    `phase`       VARCHAR(20)  NOT NULL COMMENT '阶段：VALIDATION / CANARY / RELEASE',
    `batch_no`    INT          NOT NULL COMMENT '批次号',
    `ratio`       DECIMAL(5,2)          DEFAULT NULL COMMENT '放量比例',
    `target_expr` VARCHAR(512)          DEFAULT NULL COMMENT '目标表达式',
    `state`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '批次状态：PENDING / ACTIVE / COMPLETED / PAUSED',
    `released_at` DATETIME              DEFAULT NULL COMMENT '放量时间',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_task` (`task_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='灰度批次表';

-- 7.7 tb_task_metric (US-032)
CREATE TABLE IF NOT EXISTS `tb_task_metric`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`        BIGINT       NOT NULL COMMENT '升级任务ID',
    `batch_no`       INT          NOT NULL COMMENT '批次号',
    `success_cnt`    INT          NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_cnt`       INT          NOT NULL DEFAULT 0 COMMENT '失败数',
    `timeout_cnt`    INT          NOT NULL DEFAULT 0 COMMENT '超时数',
    `fail_rate`      DECIMAL(5,4)          DEFAULT NULL COMMENT '失败率',
    `gate_threshold` DECIMAL(5,4)          DEFAULT NULL COMMENT '门禁阈值',
    `gate_state`     VARCHAR(20)  NOT NULL DEFAULT 'OK' COMMENT '门禁状态：OK / BREACH',
    `stat_time`      DATETIME              DEFAULT NULL COMMENT '统计时间',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`      VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`      VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`    INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`      TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_task_batch` (`task_id`, `batch_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='批次健康度门禁指标表';

-- 7.8 tb_task_report (US-033)
CREATE TABLE IF NOT EXISTS `tb_task_report`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`         BIGINT       NOT NULL COMMENT '升级任务ID',
    `complete_rate`   DECIMAL(5,4)          DEFAULT NULL COMMENT '完成率',
    `success_rate`    DECIMAL(5,4)          DEFAULT NULL COMMENT '成功率',
    `fail_case_dist`  TEXT                  DEFAULT NULL COMMENT '失败case分布（JSON）',
    `gen_time`        DATETIME              DEFAULT NULL COMMENT '生成时间',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`       VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`     INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`       TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_task` (`task_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='任务报告表';

-- 7.9 tb_user_consent (US-034)
CREATE TABLE IF NOT EXISTS `tb_user_consent`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`        BIGINT       NOT NULL COMMENT '升级任务ID',
    `vin`            VARCHAR(20)  NOT NULL COMMENT '车架号',
    `consent_type`   VARCHAR(32)  NOT NULL COMMENT '授权类型：NOTICE / TERMS / PRIVACY',
    `article_id`     BIGINT                DEFAULT NULL COMMENT '文章ID',
    `consent_result` TINYINT      NOT NULL COMMENT '授权结果：0-拒绝，1-同意',
    `consent_time`   DATETIME     NOT NULL COMMENT '授权时间',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`      VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`      VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`    INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`      TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_task_vin` (`task_id`, `vin`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户授权同意表';

-- 7.10 tb_upgrade_log (US-035)
CREATE TABLE IF NOT EXISTS `tb_upgrade_log`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`      BIGINT       NOT NULL COMMENT '升级任务ID',
    `vin`          VARCHAR(20)  NOT NULL COMMENT '车架号',
    `log_url`      VARCHAR(512)          DEFAULT NULL COMMENT '日志URL（对象存储）',
    `upload_state` VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '上传状态：PENDING / UPLOADING / SUCCESS / FAIL',
    `upload_time`  DATETIME              DEFAULT NULL COMMENT '上传时间',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`    VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`  INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`    TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_task_vin` (`task_id`, `vin`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='升级日志登记表';

-- 7.11 tb_regulatory_filing (US-040)
CREATE TABLE IF NOT EXISTS `tb_regulatory_filing`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`      BIGINT       NOT NULL COMMENT '升级活动ID',
    `filing_type`      VARCHAR(32)  NOT NULL COMMENT '备案类型',
    `sw_content_ref`   VARCHAR(512)          DEFAULT NULL COMMENT '软件内容引用',
    `release_note_ref` VARCHAR(512)          DEFAULT NULL COMMENT 'ReleaseNote引用',
    `filing_status`    VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '备案状态：PENDING / FILED / APPROVED / REJECTED',
    `filing_no`        VARCHAR(255)          DEFAULT NULL COMMENT '备案编号',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`        VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`      INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`        TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='监管备案表';

-- 7.12 tb_approved_sw_manifest (US-046)
CREATE TABLE IF NOT EXISTS `tb_approved_sw_manifest`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `manifest_code`   VARCHAR(64)  NOT NULL COMMENT '快照编码（幂等键）',
    `activity_id`     BIGINT       NOT NULL COMMENT '升级活动ID',
    `swin_code`       VARCHAR(64)  NOT NULL COMMENT 'SWIN代码',
    `rxswin_value`    VARCHAR(128)          DEFAULT NULL COMMENT 'RXSWIN值（MDM回写）',
    `manifest_status` VARCHAR(32)  NOT NULL DEFAULT 'FROZEN' COMMENT '快照状态：FROZEN',
    `approve_time`    DATETIME              DEFAULT NULL COMMENT '批准时间',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`       VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`     INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`       TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_manifest_code` (`manifest_code`),
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='型式批准软件版本组合快照表（RXSWIN manifest）';

-- 7.13 tb_approved_sw_manifest_item (US-046)
CREATE TABLE IF NOT EXISTS `tb_approved_sw_manifest_item`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `manifest_id`       BIGINT       NOT NULL COMMENT '快照ID',
    `vehicle_node_code` VARCHAR(64)  NOT NULL COMMENT '车载节点代码',
    `part_code`         VARCHAR(64)  NOT NULL COMMENT '软件零件号',
    `approved_version`  VARCHAR(255) NOT NULL COMMENT '批准版本',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_manifest` (`manifest_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='快照明细表';
