ALTER TABLE `tb_vehicle_part`
    ADD COLUMN `device_item` VARCHAR(20) DEFAULT NULL COMMENT '设备项' AFTER `device_code`;

ALTER TABLE `tb_vehicle_part_history`
    ADD COLUMN `device_item` VARCHAR(20) DEFAULT NULL COMMENT '设备项' AFTER `device_code`;