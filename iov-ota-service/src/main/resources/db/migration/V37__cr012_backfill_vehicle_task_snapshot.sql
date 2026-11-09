-- ============================================================
-- IOV-OTA-DSN-CR-012 §2.2：回填已发布任务缺失的 VehicleTask 快照字段
--
-- 背景：
--   修复前发布任务只写入 tb_task_vehicle 的 (activity_id, task_id, vin)，
--   vehicle_task_status / release_at / vt_start_time / vt_end_time / task_revision /
--   consent_* / download_ready_state 全部为 NULL。
--   而读取侧 VehicleTaskMapper.selectByVin / selectByTaskId 均以
--   "vehicle_task_status IS NOT NULL" 作为过滤条件，导致已发布任务的车辆任务
--   对车端任务检测与管理端 VehicleTask 查询不可见（表现为「发布后没有产生车辆任务」）。
--
-- 范围：
--   仅回填 vehicle_task_status 为 NULL 的行；已有运行态（已下载/已安装/已终态）的行不覆盖。
--   发布时刻优先取 tb_task.actual_release_time，其次 release_time，最后车辆任务创建时间。
--   任务已取消/已取代的，同步回填为对应终态，避免回填后被误判为可下载。
-- ============================================================

UPDATE `tb_task_vehicle` tv
    JOIN `tb_task` t ON t.id = tv.task_id
SET tv.vehicle_task_status = CASE
        WHEN t.state = 9 THEN 'CANCELED'
        WHEN t.state = 10 THEN 'SUPERSEDED'
        ELSE 'DOWNLOAD_PENDING'
    END,
    tv.task_revision         = COALESCE(tv.task_revision, 1),
    tv.download_ready_state  = COALESCE(tv.download_ready_state, 'NOT_STARTED'),
    tv.consent_required      = COALESCE(tv.consent_required, 0),
    tv.consent_state         = COALESCE(tv.consent_state, 'NOT_REQUIRED'),
    tv.release_at            = COALESCE(tv.release_at, t.actual_release_time, t.release_time, tv.create_time),
    tv.vt_start_time         = COALESCE(tv.vt_start_time, t.start_time),
    tv.vt_end_time           = COALESCE(tv.vt_end_time, t.end_time),
    tv.last_attempt_no       = COALESCE(tv.last_attempt_no, 0)
WHERE tv.vehicle_task_status IS NULL
  AND tv.row_valid = 1;
