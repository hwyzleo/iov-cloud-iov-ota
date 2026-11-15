package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台车辆任务重置结果（IOV-OTA-DSN-CR-025 §4.29.6）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVehicleResetResultMpt {

    /** 原车辆任务ID（已置 SUPERSEDED） */
    private Long oldVehicleTaskId;

    /** 新车辆任务ID（初始化状态） */
    private Long newVehicleTaskId;

    /** 原车辆任务最终状态（恒为 SUPERSEDED） */
    private String oldStatus;

    /** 是否强制收口了活动执行 */
    private Boolean forcedExecutionCanceled;

    /** 被强制收口的活动执行ID */
    private Long oldExecutionId;

    /** 被强制收口前的执行状态 */
    private String oldExecutionStatus;
}
