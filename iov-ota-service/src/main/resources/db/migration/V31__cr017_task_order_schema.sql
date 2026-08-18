-- CR-017: Task 波次排号与前序关系自动推导
-- 设计增量见 IOV-OTA-DSN-CR-017 §4.1 / §8。
-- 原则：序号作用域为 (activity_id, phase)，由 Task 创建应用服务在 Activity 行锁下并发分配；
--       删除 CR-015（V28）遗留的 (activity_id, sequence_no) 唯一键（作用域错误，
--       会阻止同一活动内不同 phase 各自从 0 排号）；
--       增加 (activity_id, phase, sequence_no) 普通索引作为前序查询与冲突检测保障；
--       历史数据可能含重复 sequence，SHALL NOT 为了建唯一索引而强制改写已发布/已执行事实，
--       先保留普通索引，待业务确认清理后再升级唯一约束。

-- 1. 删除旧作用域唯一键（CR-015 遗留）
ALTER TABLE `tb_task`
    DROP INDEX `uk_activity_sequence`;

-- 2. 增加 (activity_id, phase, sequence_no) 普通索引（前序推导查询 + 唯一性冲突检测）
ALTER TABLE `tb_task`
    ADD KEY `idx_task_activity_phase_sequence` (`activity_id`, `phase`, `sequence_no`);
