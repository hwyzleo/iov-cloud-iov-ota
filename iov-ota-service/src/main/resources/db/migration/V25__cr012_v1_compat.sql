-- CR-012: 兼容迁移（§9）
-- 新增任务最小协议版本字段，用于 v1/v2 能力准入筛选

ALTER TABLE `tb_task`
    ADD COLUMN `minimum_protocol_version` VARCHAR(20) NULL DEFAULT '2.0' COMMENT '最小协议版本（CR-012 §9.4，新任务按此和车辆能力准入）' AFTER `state_before_pause`;
