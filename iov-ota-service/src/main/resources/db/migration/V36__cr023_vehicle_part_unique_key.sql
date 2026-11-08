-- ============================================================
-- IOV-OTA-DSN-CR-023 §7：车辆零件投影业务身份唯一键（上线门禁）
--
-- 背景：
--   vin + pn 是零件投影业务身份键（RD-023-7）。DB 层加唯一键阻止
--   「先查再插」并发产生的重复行；现有写入链路均使用物理删除，
--   唯一键覆盖全表（含 row_valid = 0，A1 策略）。
--
-- 上线门禁（B2 策略，硬失败）：
--   1) 执行前必须按 docs/cr023_vehicle_part_duplicate_scan.sql 扫描并
--      人工清理重复 (vin, pn)；
--   2) 若仍存在重复，本 ALTER 会因 Duplicate entry 失败，迁移中止；
--      这是预期门禁，不得在脚本中自动删除、合并或改写历史行；
--      清理数据后执行 flyway repair 并重跑本迁移；
--   3) 本脚本只包含唯一键 DDL，避免与其他 DDL 混合导致半成品。
--
-- 注意：运行环境 Druid WallFilter 会拦截 PREPARE / SIGNAL / 存储过程，
--       因此门禁依赖唯一键 ALTER 本身的失败语义，不使用动态 SQL。
-- ============================================================

ALTER TABLE `tb_vehicle_part`
    ADD UNIQUE KEY `uk_vehicle_part_vin_pn` (`vin`, `pn`);
