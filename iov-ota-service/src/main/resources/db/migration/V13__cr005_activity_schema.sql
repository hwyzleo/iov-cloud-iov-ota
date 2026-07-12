-- CR-005: 升级活动本体字段扩展 + 多级审批表

-- 清理旧 TEXT 数据（upgrade_purpose 原 TEXT 列存有文本值，无法直接转 TINYINT）
UPDATE `tb_activity` SET `upgrade_purpose` = NULL WHERE `upgrade_purpose` IS NOT NULL;

-- ALTER tb_activity: 新增 CR-005 活动本体字段 + 修改 upgrade_purpose 类型
ALTER TABLE `tb_activity`
    ADD COLUMN `activity_code`                  VARCHAR(255)          DEFAULT NULL COMMENT '活动编码（系统生成·全局唯一·不可变）' AFTER `name`,
    ADD COLUMN `size_calc_time`                  DATETIME              DEFAULT NULL COMMENT '文件大小缓存计算时间' AFTER `total_file_size`,
    ADD COLUMN `is_type_approval_relevant`       TINYINT(1)            DEFAULT 0   COMMENT '是否型批相关',
    ADD COLUMN `type_approval_assessment_state`  TINYINT               DEFAULT 0   COMMENT '型批影响评估状态：0 未评估 1 通过 2 阻断',
    ADD COLUMN `release_note_article_id`         BIGINT                DEFAULT NULL COMMENT '发行说明文章ID',
    ADD COLUMN `notice_consent_required`         TINYINT(1)            DEFAULT 0   COMMENT '须知是否需显式同意',
    ADD COLUMN `terms_consent_required`          TINYINT(1)            DEFAULT 0   COMMENT '条款是否需显式同意',
    ADD COLUMN `privacy_consent_required`        TINYINT(1)            DEFAULT 0   COMMENT '隐私是否需显式同意',
    ADD COLUMN `rxswin`                          VARCHAR(255)          DEFAULT NULL COMMENT 'RXSWIN值（只读·manifest回填·仅1:1场景）',
    ADD UNIQUE INDEX `uk_activity_code` (`activity_code`) USING BTREE,
    MODIFY COLUMN `upgrade_purpose` TINYINT DEFAULT NULL COMMENT '升级目的：1 缺陷修复 2 功能新增 3 安全补丁 4 合规整改 9 其他';

-- CREATE tb_activity_approval: 活动多级审批表
CREATE TABLE IF NOT EXISTS `tb_activity_approval`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id`     BIGINT       NOT NULL COMMENT '升级活动ID',
    `approval_stage`  VARCHAR(32)  NOT NULL COMMENT '审批阶段：QUALITY / PRODUCT / SECURITY',
    `approver_id`     VARCHAR(64)           DEFAULT NULL COMMENT '审批人ID',
    `result`          VARCHAR(16)  NOT NULL COMMENT '审批结果：PASS / REJECT',
    `comment`         VARCHAR(512)          DEFAULT NULL COMMENT '审批意见',
    `approve_time`    DATETIME              DEFAULT NULL COMMENT '审批时间',
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`       VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`     INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`       TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activity` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='升级活动多级审批表';
