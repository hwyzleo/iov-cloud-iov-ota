-- 修复: V24 CR-012 建表时遗漏了 BasePo 约定的 description 列
-- 所有被 BasePo 子类 PO 映射的 CR-012 表（含 V30 重建的 tb_vehicle_task_consent）
-- 均需补充 description 列，否则 MyBatis-Plus 生成的 SELECT 会报 Unknown column 'description'
-- （同类问题参见 V12__add_description_to_cr004_tables.sql 的修复先例）。

ALTER TABLE `tb_vehicle_inventory`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `accepted_time`;

ALTER TABLE `tb_vehicle_inventory_item`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `hardware_version`;

ALTER TABLE `tb_vehicle_task_snapshot`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `snapshot_content`;

ALTER TABLE `tb_vehicle_task_consent`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `source_model`;

ALTER TABLE `tb_vehicle_task_package`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `verify_state`;

ALTER TABLE `tb_package_stage_result`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `fail_reason`;

ALTER TABLE `tb_task_vehicle_execution`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `end_time`;

ALTER TABLE `tb_task_vehicle_execution_event`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `received_time`;

ALTER TABLE `tb_task_vehicle_execution_control`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `reason`;

ALTER TABLE `tb_task_vehicle_execution_control_ack`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `ack_time`;

ALTER TABLE `tb_task_vehicle_execution_ecu_result`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `fail_reason`;
