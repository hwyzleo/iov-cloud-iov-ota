-- ============================================================
-- IOV-OTA-DSN-CR-023 §4.2：管理端全局车辆任务列表排序索引（前向迁移）
--
-- 背景：
--   /api/mpt/taskVehicle/v1/list 在 vin、vehicleTaskStatus 均缺省时退化为
--   全表过滤 + ORDER BY tv.create_time DESC, tv.id DESC。
--   tb_task_vehicle 仅有 idx_activity / idx_task / idx_vin，无法支撑该排序，
--   会走全表扫描 + filesort（Using temporary; Using filesort）。
--
-- 说明：
--   1) 仅使用静态 DDL，禁止 PREPARE / 存储过程 / SIGNAL：
--      运行环境 Druid WallFilter 会拦截 MySqlPrepareStatement；
--   2) InnoDB 二级索引隐式携带主键，故 (row_valid, create_time) 等价于
--      (row_valid, create_time, id)，可同时满足 WHERE row_valid = 1 与
--      ORDER BY create_time DESC, id DESC 的双向有序扫描；
--   3) 不加显式 id 列，避免冗余索引体积。
-- ============================================================

ALTER TABLE `tb_task_vehicle`
    ADD INDEX `idx_task_vehicle_valid_create` (`row_valid`, `create_time`);
