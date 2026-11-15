package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 车辆任务重置结果（IOV-OTA-DSN-CR-025 §4.29.6）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleTaskResetResult {

    /** 原车辆任务ID（已置 SUPERSEDED） */
    private Long oldVehicleTaskId;

    /** 新车辆任务ID（初始化状态） */
    private Long newVehicleTaskId;

    /** 原车辆任务最终状态（恒为 SUPERSEDED） */
    private String oldStatus;

    /** 是否强制收口了活动执行 */
    private boolean forcedExecutionCanceled;

    /** 被强制收口的活动执行ID（无活动执行时为 null） */
    private Long oldExecutionId;

    /** 被强制收口前执行状态（收口后为 CANCELED） */
    private String oldExecutionStatus;
}
