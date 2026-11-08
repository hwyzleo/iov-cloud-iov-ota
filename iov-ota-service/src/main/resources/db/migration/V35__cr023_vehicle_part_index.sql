-- ============================================================
-- IOV-OTA-DSN-CR-023 §7：车辆零件投影查询索引（前向迁移）
--
-- 说明：
--   1) 仅使用静态 DDL，禁止 PREPARE / 存储过程 / SIGNAL：
--      运行环境 Druid WallFilter 会拦截 MySqlPrepareStatement；
--   2) 本脚本只负责普通索引 (vin, device_code)，支撑按 VIN 一次批量
--      查询零件并在内存按 ECU / VMD vehicleNodeCode 分组；
--   3) vin + pn 唯一键单独在 V36 迁移中创建，便于重复数据清理后
--      只重跑唯一键脚本，避免 DDL 非事务导致重复建普通索引。
-- ============================================================

ALTER TABLE `tb_vehicle_part`
    ADD INDEX `idx_vehicle_part_vin_device` (`vin`, `device_code`);
