-- CR-022: 车辆软件清单最后上报时间运行态读模型（IOV-OTA-DSN-CR-022 §3）
-- 原则：
--   1) 每 VIN 一行运行态，仅保存最后有效受理时间（last_reported_at），不保存车辆主档副本；
--   2) 不新增 last_mode/last_revision 等字段，避免形成第二清单事实来源；
--   3) tb_vehicle_inventory 继续保存 FULL 历史事实与 accepted_time，本表不改变其语义；
--   4) 仅使用前向迁移，历史脚本不得改写。

-- ============================================================
-- 1. 新建 tb_vehicle_inventory_report_state
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_vehicle_inventory_report_state` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`              VARCHAR(20)  NOT NULL COMMENT '车架号，运行态业务唯一键',
    `last_reported_at` DATETIME(3)  NOT NULL COMMENT '最后一次成功受理车辆软件清单上报的服务端时间',
    `description`      VARCHAR(255) NULL COMMENT '描述',
    `create_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`        BIGINT       NULL COMMENT '创建人',
    `modify_time`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`        BIGINT       NULL COMMENT '修改人',
    `row_version`      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_inventory_report_state_vin` (`vin`),
    KEY `idx_vehicle_inventory_report_state_time` (`last_reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA车辆软件清单上报运行态';

-- ============================================================
-- 2. 历史初始化（幂等，且不回退已存在的较新 last_reported_at）
--    仅以既有 FULL 事实的 MAX(accepted_time) 初始化，历史 DIGEST/业务重放无独立时间事实，不得伪造。
-- ============================================================
INSERT INTO `tb_vehicle_inventory_report_state`
    (`vin`, `last_reported_at`, `create_time`, `modify_time`, `row_version`, `row_valid`)
SELECT vin, MAX(accepted_time), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 1
FROM `tb_vehicle_inventory`
WHERE row_valid = 1
  AND accepted_time IS NOT NULL
GROUP BY vin
ON DUPLICATE KEY UPDATE
    `last_reported_at` = GREATEST(`last_reported_at`, VALUES(`last_reported_at`)),
    `modify_time` = CURRENT_TIMESTAMP(3);
