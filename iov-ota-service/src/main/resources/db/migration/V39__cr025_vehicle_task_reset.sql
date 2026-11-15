-- ============================================================
-- IOV-OTA-DSN-CR-025 §4.29：车辆任务重置（US-124）前向迁移
--
-- 内容：
--   1) 新增 tb_task_vehicle_reset_log：车辆任务重置不可变审计（追加写）；
--   2) tb_task_vehicle 新增当前性查询索引 idx_task_vehicle_current
--      支撑 current = (task_id, vin, superseded_by IS NULL) 的定位。
--
-- 约束与说明：
--   1) 仅静态 DDL，禁止 PREPARE / SIGNAL / 存储过程：运行环境 Druid WallFilter
--      会拦截 MySqlPrepareStatement（与 V35/V36/V38 约定一致）；
--   2) 不建立 (task_id, vin) 唯一索引：当前性由 superseded_by IS NULL 表达，
--      历史行必须保留 SUPERSEDED 事实，唯一性由父任务行锁 + 应用层校验保证；
--   3) 不做任何数据清洗、不回填、不改写历史迁移脚本；
--   4) 列名使用 force_flag 而非保留字 force，避免 ORM 生成 SQL 与关键字冲突。
-- ============================================================

CREATE TABLE IF NOT EXISTS `tb_task_vehicle_reset_log` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `old_vehicle_task_id`  BIGINT       NOT NULL COMMENT '原车辆任务ID（已置 SUPERSEDED）',
    `new_vehicle_task_id`  BIGINT       NOT NULL COMMENT '新车辆任务ID（初始化状态）',
    `task_id`              BIGINT       NOT NULL COMMENT '升级任务ID',
    `activity_id`          BIGINT       NULL COMMENT '升级活动ID',
    `vin`                  VARCHAR(20)  NOT NULL COMMENT '车架号',
    `old_status`           VARCHAR(30)  NOT NULL COMMENT '重置前车辆任务状态',
    `old_download_ready`   VARCHAR(30)  NULL COMMENT '重置前下载准备状态',
    `old_consent_state`    VARCHAR(30)  NULL COMMENT '重置前授权状态',
    `old_attempt_no`       INT          NULL COMMENT '重置前尝试序号',
    `old_execution_id`     BIGINT       NULL COMMENT '被强制收口的活动执行ID',
    `old_execution_status` VARCHAR(30)  NULL COMMENT '被强制收口前执行状态',
    `force_flag`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否强制执行中重置',
    `reason`               VARCHAR(512) NOT NULL COMMENT '重置原因',
    `result`               VARCHAR(20)  NOT NULL COMMENT '结果：SUCCESS/FAILED',
    `create_time`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`            VARCHAR(64)  NULL COMMENT '操作人',
    `modify_time`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `modify_by`            VARCHAR(64)  NULL COMMENT '修改人',
    `row_version`          BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `row_valid`            TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_tvrl_old` (`old_vehicle_task_id`),
    KEY `idx_tvrl_new` (`new_vehicle_task_id`),
    KEY `idx_tvrl_task_vin` (`task_id`, `vin`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆任务重置审计';

ALTER TABLE `tb_task_vehicle`
    ADD INDEX `idx_task_vehicle_current` (`task_id`, `vin`, `superseded_by`);
