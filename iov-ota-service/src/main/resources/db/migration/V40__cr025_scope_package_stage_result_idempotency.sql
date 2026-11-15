-- ============================================================
-- CR-026: 包阶段结果幂等键按车辆任务隔离
-- ============================================================
-- 车端 stage_result_id 由「包身份 + 结果内容」确定性派生（重试同一结果同 ID），
-- 因此车辆任务重置（CR-025）后新一轮复用同一 ID。旧实现按全局 stage_result_id 判重，
-- 新车辆任务的阶段结果被整体误判为重复而丢弃：tb_package_stage_result 无新任务行，
-- 「全部必需包校验成功」永不成立，车辆任务停在 DOWNLOAD_PENDING，安装许可被永久拒绝
-- （实车 2026-11-15：车辆任务 3 EXECUTING 重置为车辆任务 4 后，4 个包 DECRYPT SUCCESS
-- 全部被幂等跳过，16:23 起持续申请安装许可被拒）。
-- 幂等语义保持不变：同一车辆任务内重复上报仍然只有一行。
-- ============================================================

ALTER TABLE `tb_package_stage_result`
    DROP INDEX `uk_stage_result_id`,
    ADD UNIQUE KEY `uk_vehicle_task_stage_result_id` (`vehicle_task_id`, `stage_result_id`);
