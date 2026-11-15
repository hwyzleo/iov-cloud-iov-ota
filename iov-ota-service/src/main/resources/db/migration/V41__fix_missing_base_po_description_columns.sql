-- ============================================================
-- 修复: V13 / V33 / V39 建表时遗漏了 BasePo 约定的 description 列
--
-- 现象：PO 继承 framework-mysql 的 BasePo（字段 description/createBy/createTime/
-- modifyBy/modifyTime/rowVersion/rowValid），MyBatis-Plus 对
-- BaseMapper.selectList / selectById 生成全字段 SELECT，表缺列时直接抛
--   java.sql.SQLSyntaxErrorException: Unknown column 'description' in 'field list'
-- 管理后台「车辆任务升级过程」即因此 500（TaskVehicleResetLogPo -> tb_task_vehicle_reset_log）。
--
-- 约束与说明：
--   1) 追加列修复，不改写任何历史迁移脚本（V39 已执行，Flyway validate-on-migrate
--      会校验 checksum）；
--   2) 仅静态 DDL，禁止 PREPARE / SIGNAL / 存储过程（Druid WallFilter 拦截，
--      与 V35 / V36 / V38 / V39 约定一致）；
--   3) 只新增可空列，不触碰业务数据，不回填、不改写历史事实；
--   4) 同类修复先例：V12__add_description_to_cr004_tables.sql、
--      V32__cr012_add_description_columns.sql。
-- ============================================================

-- 1. CR-025 车辆任务重置审计（本次报错表）
ALTER TABLE `tb_task_vehicle_reset_log`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `result`;

-- 2. CR-019 车辆 ECU 清单软件单元子表
ALTER TABLE `tb_vehicle_inventory_software_unit`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_valid`;

-- 3. CR-019 安装执行 ECU 软件单元结果子表
ALTER TABLE `tb_task_vehicle_execution_ecu_software_unit`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_valid`;

-- 4. CR-005 活动多级审批表
ALTER TABLE `tb_activity_approval`
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_valid`;
