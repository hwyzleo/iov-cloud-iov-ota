-- DSN-CR-004: 软件内部版本发布生命周期 + 软件包制品状态 + 双重发布门禁

-- === A. tb_software_build_version 字段变更 ===

-- 1. 新增 build_state（发布工作流状态）
ALTER TABLE `tb_software_build_version`
    ADD COLUMN `build_state` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布工作流状态: DRAFT/TESTING/RELEASED/DEPRECATED/RETIRED' AFTER `software_source`;

-- 2. 新增 release_time（发布时间）
ALTER TABLE `tb_software_build_version`
    ADD COLUMN `release_time` DATETIME DEFAULT NULL COMMENT '发布时间' AFTER `release_date`;

-- 3. software_desc -> change_note（技术变更说明）
ALTER TABLE `tb_software_build_version`
    CHANGE COLUMN `software_desc` `change_note` VARCHAR(512) DEFAULT NULL COMMENT '技术变更说明';

-- === B. tb_software_package 新增 package_state ===
ALTER TABLE `tb_software_package`
    ADD COLUMN `package_state` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '制品可用性状态: ACTIVE/DEPRECATED/REVOKED/RETIRED' AFTER `ota`;

-- === C. 新建测试报告子表 ===
CREATE TABLE IF NOT EXISTS `tb_software_build_version_test_report`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sbv_id`      BIGINT       NOT NULL COMMENT '软件内部版本ID',
    `report_url`  VARCHAR(512) NOT NULL COMMENT '报告地址',
    `report_type` VARCHAR(50)           DEFAULT NULL COMMENT '报告类型',
    `test_state`  VARCHAR(20)           DEFAULT NULL COMMENT '测试状态',
    `verdict`     VARCHAR(20)           DEFAULT NULL COMMENT '测试结论',
    `tested_at`   DATETIME              DEFAULT NULL COMMENT '测试时间',
    `tested_by`   VARCHAR(64)           DEFAULT NULL COMMENT '执行方',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_sbv_id` (`sbv_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件内部版本测试报告';

-- === D. 新建软硬件适配矩阵子表 ===
CREATE TABLE IF NOT EXISTS `tb_software_build_version_adaptation`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sbv_id`               BIGINT       NOT NULL COMMENT '软件内部版本ID',
    `hardware_assembly_pn` VARCHAR(50)  NOT NULL COMMENT '硬件总成零件号',
    `hardware_ver`         VARCHAR(255)          DEFAULT NULL COMMENT '硬件版本号',
    `adaptive_level`       SMALLINT              DEFAULT NULL COMMENT '适配级别: 1-LE,2-GE,3-EQ',
    `sort`                 INT                   DEFAULT 0 COMMENT '排序',
    `create_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`            VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`          INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`            TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_sbv_id` (`sbv_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='软件内部版本软硬件适配矩阵';

-- === E. 数据迁移：software_report -> 测试报告子表 ===
INSERT INTO `tb_software_build_version_test_report` (`sbv_id`, `report_url`, `report_type`)
SELECT `id`, `software_report`, 'GENERAL'
FROM `tb_software_build_version`
WHERE `software_report` IS NOT NULL AND `software_report` != '';

-- === F. 数据迁移：adaptive_assembly_pn -> 适配矩阵子表 ===
INSERT INTO `tb_software_build_version_adaptation` (`sbv_id`, `hardware_assembly_pn`)
SELECT `id`, `adaptive_assembly_pn`
FROM `tb_software_build_version`
WHERE `adaptive_assembly_pn` IS NOT NULL AND `adaptive_assembly_pn` != '';

-- === G. 删除已迁移的列 ===
ALTER TABLE `tb_software_build_version` DROP COLUMN `software_report`;
ALTER TABLE `tb_software_build_version` DROP COLUMN `adaptive_assembly_pn`;
ALTER TABLE `tb_software_build_version` DROP COLUMN `adaptive_software_pn`;
