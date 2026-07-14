-- CR-006: 升级对象统一建模、分组与组策略
-- 1. 创建 tb_activity_upgrade_target 表（统一升级对象）
-- 2. 创建 tb_activity_group_policy 表（组策略）
-- 3. 数据迁移：从 tb_activity_software_build_version、tb_activity_target_version、tb_activity_install_order 迁移
-- 4. 删除旧表

-- 1. 创建 tb_activity_upgrade_target 表
CREATE TABLE IF NOT EXISTS `tb_activity_upgrade_target` (
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`               BIGINT       NOT NULL COMMENT '活动ID',
    `source_type`               TINYINT      NOT NULL DEFAULT 0 COMMENT '来源类型：0 手动，1 基线',
    `baseline_code`             VARCHAR(255)          DEFAULT NULL COMMENT '基线代码（source_type=1时必填）',
    `vehicle_node_code`         VARCHAR(64)           DEFAULT NULL COMMENT '车载节点代码',
    `part_code`                 VARCHAR(64)           DEFAULT NULL COMMENT '软件零件号',
    `software_build_version_id` BIGINT                DEFAULT NULL COMMENT '软件内部版本ID（可空占位）',
    `critical`                  TINYINT               DEFAULT NULL COMMENT '是否关键版本（可空）',
    `ota`                       TINYINT               DEFAULT NULL COMMENT '是否支持OTA（可空）',
    `install_seq`               INT          NOT NULL DEFAULT 0 COMMENT '安装顺序号（原sort）',
    `parallel_group`            INT                   DEFAULT NULL COMMENT '并行组号（同组并行执行）',
    `group_no`                  INT                   DEFAULT NULL COMMENT '活动内分组号（0或空=独立，原version_group）',
    `force_upgrade`             TINYINT               DEFAULT 0 COMMENT '是否强制升级',
    `description`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`               INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                 TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_activity` (`activity_id`),
    INDEX `idx_source_type` (`source_type`),
    INDEX `idx_baseline_code` (`baseline_code`),
    INDEX `idx_group_no` (`group_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='活动升级对象表（统一承载基线/非基线目标版本、顺序与分组）';

-- 2. 创建 tb_activity_group_policy 表（组策略）
CREATE TABLE IF NOT EXISTS `tb_activity_group_policy` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`       BIGINT       NOT NULL COMMENT '升级活动ID',
    `group_no`          INT          NOT NULL COMMENT '活动内分组号',
    `rollback_together` TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否同升同降：1-是，0-否',
    `atomic_activation` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否原子激活：1-是，0-否',
    `unified_reboot`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否统一重启：1-是，0-否',
    `failure_policy`    TINYINT      NOT NULL DEFAULT 0 COMMENT '失败策略：0 全组回滚，1 保持旧版，2 重试后回滚',
    `fail_threshold`    INT                   DEFAULT NULL COMMENT '失败阈值（失败数量达到阈值触发策略）',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_group` (`activity_id`, `group_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='活动分组策略表（键 activity_id + group_no，组内共享）';

-- 3. 数据迁移：从 tb_activity_software_build_version 迁移到 tb_activity_upgrade_target
-- 将现有软件内部版本关系迁移为手动来源的升级对象
INSERT INTO `tb_activity_upgrade_target` (
    `activity_id`,
    `source_type`,
    `vehicle_node_code`,
    `part_code`,
    `software_build_version_id`,
    `critical`,
    `ota`,
    `install_seq`,
    `parallel_group`,
    `group_no`,
    `force_upgrade`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
)
SELECT 
    `activity_id`,
    0 AS `source_type`,  -- 手动来源
    NULL AS `vehicle_node_code`,  -- 旧表无此字段，后续需要关联 software_build_version 获取
    NULL AS `part_code`,  -- 旧表无此字段，后续需要关联 software_build_version 获取
    `software_build_version_id`,
    `critical`,
    `ota`,
    `sort` AS `install_seq`,
    NULL AS `parallel_group`,  -- 旧表无此字段
    `version_group` AS `group_no`,
    `force_upgrade`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
FROM `tb_activity_software_build_version`
WHERE `row_valid` = 1;

-- 4. 数据迁移：从 tb_activity_target_version 迁移到 tb_activity_upgrade_target
-- 将目标版本迁移为手动来源的升级对象（补充 baseline_code、vehicle_node_code、part_code）
INSERT INTO `tb_activity_upgrade_target` (
    `activity_id`,
    `source_type`,
    `baseline_code`,
    `vehicle_node_code`,
    `part_code`,
    `software_build_version_id`,
    `critical`,
    `ota`,
    `install_seq`,
    `parallel_group`,
    `group_no`,
    `force_upgrade`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
)
SELECT 
    `activity_id`,
    0 AS `source_type`,  -- 手动来源
    `baseline_code`,
    `vehicle_node_code`,
    `part_code`,
    NULL AS `software_build_version_id`,  -- 旧表存的是版本字符串，不是ID
    NULL AS `critical`,
    NULL AS `ota`,
    0 AS `install_seq`,  -- 默认顺序
    NULL AS `parallel_group`,
    NULL AS `group_no`,  -- 无分组信息
    0 AS `force_upgrade`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
FROM `tb_activity_target_version`
WHERE `row_valid` = 1;

-- 5. 数据迁移：从 tb_activity_install_order 迁移到 tb_activity_upgrade_target
-- 将安装顺序信息更新到已存在的升级对象上（通过 activity_id 和 vehicle_node_code 匹配）
-- 注意：这里假设 tb_activity_target_version 和 tb_activity_install_order 有相同的 activity_id 和 vehicle_node_code
-- 实际可能需要更复杂的匹配逻辑，这里简化处理
UPDATE `tb_activity_upgrade_target` tgt
INNER JOIN `tb_activity_install_order` ord ON tgt.`activity_id` = ord.`activity_id` 
    AND tgt.`vehicle_node_code` = ord.`vehicle_node_code`
SET tgt.`install_seq` = ord.`seq_no`,
    tgt.`parallel_group` = ord.`parallel_group`
WHERE tgt.`source_type` = 0
    AND ord.`row_valid` = 1;

-- 6. 迁移 tb_activity_dependency_group 到 tb_activity_group_policy
-- 将依赖组信息迁移到新的组策略表
INSERT INTO `tb_activity_group_policy` (
    `activity_id`,
    `group_no`,
    `rollback_together`,
    `atomic_activation`,
    `unified_reboot`,
    `failure_policy`,
    `fail_threshold`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
)
SELECT 
    `activity_id`,
    -- 需要将 group_code 转换为数字类型的 group_no
    -- 假设 group_code 是数字字符串，否则需要其他映射逻辑
    CAST(`group_code` AS UNSIGNED) AS `group_no`,
    `rollback_together`,
    0 AS `atomic_activation`,  -- 默认值
    0 AS `unified_reboot`,     -- 默认值
    0 AS `failure_policy`,     -- 默认值：全组回滚
    NULL AS `fail_threshold`,
    `description`,
    `create_time`,
    `create_by`,
    `modify_time`,
    `modify_by`,
    `row_version`,
    `row_valid`
FROM `tb_activity_dependency_group`
WHERE `row_valid` = 1;

-- 7. 删除旧表（先逻辑删除，后续可物理删除）
-- 注意：在生产环境中，建议先保留旧表，确认迁移成功后再删除
-- DROP TABLE IF EXISTS `tb_activity_software_build_version`;
-- DROP TABLE IF EXISTS `tb_activity_target_version`;
-- DROP TABLE IF EXISTS `tb_activity_install_order`;
-- DROP TABLE IF EXISTS `tb_activity_dependency_group`;

-- 8. 更新 tb_activity_upgrade_target 中的 vehicle_node_code 和 part_code
-- 通过关联 tb_software_build_version 获取设备编码和软件零件号
UPDATE `tb_activity_upgrade_target` tgt
INNER JOIN `tb_software_build_version` sbv ON tgt.`software_build_version_id` = sbv.`id`
SET tgt.`vehicle_node_code` = sbv.`device_code`,
    tgt.`part_code` = sbv.`software_pn`
WHERE tgt.`source_type` = 0
    AND tgt.`software_build_version_id` IS NOT NULL
    AND tgt.`vehicle_node_code` IS NULL;