package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆关联任务查询结果（IOV-OTA-DSN-CR-023 §4.2）
 *
 * <p>task_phase / task_state / activity_state 为数据库原始状态码，
 * 由装配层统一转换为枚举名，避免 SQL 承担展示语义。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTaskRelationResult {

    private Long vehicleTaskId;

    private String vehicleTaskStatus;

    private String resultCode;

    /** 车架号（tv.vin，全局列表无 VIN 入参时由结果回传） */
    private String vin;

    private Long taskId;

    private String taskName;

    private Integer taskPhase;

    private Integer taskState;

    private Long activityId;

    private String activityName;

    private Integer activityState;

    /** VehicleTask 发布时间快照（tv.release_at） */
    private Date releaseAt;

    /** VehicleTask 执行窗口开始快照（tv.vt_start_time） */
    private Date startTime;

    /** VehicleTask 执行窗口结束快照（tv.vt_end_time） */
    private Date endTime;

    private Date createTime;
}
