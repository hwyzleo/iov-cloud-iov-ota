-- DSN-CR-008: TA基线投影表
-- 1. Create tb_type_approval_baseline (MDM EEAD TypeApprovalBaseline projection)
-- 2. Create tb_type_approval_baseline_item (projection items)
-- 3. Add comment for approved_software_baseline degradation

-- 1. TA基线投影主表
CREATE TABLE IF NOT EXISTS `tb_type_approval_baseline`
(
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ta_baseline_code`       VARCHAR(255) NOT NULL COMMENT 'TA基线代码（业务键）',
    `swin_code`              VARCHAR(64)  NOT NULL COMMENT 'SWIN代码',
    `anchor_type`            VARCHAR(32)  NOT NULL COMMENT '锚定类型：VARIANT / MODEL',
    `anchor_code`            VARCHAR(255) NOT NULL COMMENT '锚定代码',
    `status`                 VARCHAR(32)  NOT NULL COMMENT '状态：仅消费 RELEASED / FROZEN',
    `projection_digest`      VARCHAR(128)          DEFAULT NULL COMMENT '型批版本组合摘要（sha256(sortedItems)）',
    `effective_from`         DATETIME              DEFAULT NULL COMMENT '生效时间',
    `source_baseline_scope`  VARCHAR(255)          DEFAULT NULL COMMENT '溯源：来源基线范围',
    `up_version`             BIGINT                DEFAULT NULL COMMENT '上游版本号（幂等键之一）',
    `description`            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`              VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`              VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`            INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`              TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ta_baseline_code` (`ta_baseline_code`),
    INDEX `idx_swin_code` (`swin_code`),
    INDEX `idx_anchor` (`anchor_type`, `anchor_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='型式批准基线投影表（MDM EEAD TypeApprovalBaseline）';

-- 2. TA基线投影明细表
CREATE TABLE IF NOT EXISTS `tb_type_approval_baseline_item`
(
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ta_baseline_code`       VARCHAR(255) NOT NULL COMMENT 'TA基线代码',
    `vehicle_node_code`      VARCHAR(64)  NOT NULL COMMENT '车载节点代码',
    `part_code`              VARCHAR(64)  NOT NULL COMMENT '软件零件号',
    `approved_version`       VARCHAR(128)          DEFAULT NULL COMMENT '批准版本',
    `source_baseline_code`   VARCHAR(255)          DEFAULT NULL COMMENT '溯源：来源基线代码',
    `description`            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`              VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`              VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`            INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`              TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_ta_baseline_code` (`ta_baseline_code`),
    INDEX `idx_vehicle_node_code` (`vehicle_node_code`),
    INDEX `idx_part_code` (`part_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='型式批准基线明细投影表';

-- 3. 标记 approved_software_baseline 降级为来源标注（不删列，向后兼容）
-- 注：tb_swin_managed_system.approved_software_baseline 字段保留，但自CR-008起降级为可选「来源标注」
-- SHALL NOT 作为型批基准真值参与型式批准影响评估门禁
-- 型批「上一批准基准」改读 tb_type_approval_baseline* 投影
