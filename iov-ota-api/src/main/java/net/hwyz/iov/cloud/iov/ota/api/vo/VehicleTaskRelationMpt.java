package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆关联升级任务（IOV-OTA-DSN-CR-023 §4.2）
 *
 * <p>字段来源固定：任务/活动名称与状态由关联查询一次取得，禁止逐行补全；
 * releaseAt 取 VehicleTask 发布时间快照，startTime/endTime 取 VehicleTask
 * 冻结的执行窗口快照（vt_start_time / vt_end_time），语义不得互换。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTaskRelationMpt {

    /** VehicleTask ID */
    private Long vehicleTaskId;

    /** VehicleTask 状态（VehicleTaskStatus 枚举名） */
    private String vehicleTaskStatus;

    /** VehicleTask 结果代码 */
    private String resultCode;

    /** 升级任务ID */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 任务阶段（TaskPhase 枚举名） */
    private String taskPhase;

    /** 任务状态（TaskState 枚举名） */
    private String taskState;

    /** 升级活动ID */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 活动状态（ActivityState 枚举名） */
    private String activityState;

    /** VehicleTask 发布时间快照 */
    private Date releaseAt;

    /** VehicleTask 执行窗口开始时间快照 */
    private Date startTime;

    /** VehicleTask 执行窗口结束时间快照 */
    private Date endTime;

    /** VehicleTask 创建时间 */
    private Date createTime;
}
