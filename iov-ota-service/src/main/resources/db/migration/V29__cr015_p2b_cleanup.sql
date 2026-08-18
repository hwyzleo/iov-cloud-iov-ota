-- CR-015 P2-B: 清理重复制品/批次设计
-- 设计增量见 IOV-OTA-DSN-CR-015 §3.1 / §9-P2-B。
-- 原则：SoftwarePackage 是唯一物理制品 SSOT；升级包不实现第二套制品记录；
--       放量模型为多任务波次（sequence_no + previous_task_id），不使用批次；
--       确认无有效业务引用后删除重复表。

-- 可下发升级包（重复制品记录，不进入目标实现）-> 删除
DROP TABLE IF EXISTS `tb_upgrade_package`;

-- 异步构建任务（升级包重复设计的一部分）-> 删除
DROP TABLE IF EXISTS `tb_upgrade_package_build`;

-- 灰度批次（放量模型已由多任务波次取代，task_batch 不再承载业务）-> 删除
DROP TABLE IF EXISTS `tb_task_batch`;
