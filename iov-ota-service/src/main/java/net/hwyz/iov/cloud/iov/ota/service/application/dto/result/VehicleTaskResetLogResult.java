package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 车辆任务重置审计摘要（IOV-OTA-DSN-CR-025 §4.29.7）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleTaskResetLogResult {

    /** 审计记录ID */
    private Long id;

    /** 原车辆任务ID（已置 SUPERSEDED） */
    private Long oldVehicleTaskId;

    /** 新车辆任务ID（初始化状态） */
    private Long newVehicleTaskId;

    /** 重置前车辆任务状态 */
    private String oldStatus;

    /** 重置前下载准备状态 */
    private String oldDownloadReady;

    /** 重置前授权状态 */
    private String oldConsentState;

    /** 重置前尝试序号 */
    private Integer oldAttemptNo;

    /** 被强制收口的活动执行ID */
    private Long oldExecutionId;

    /** 被强制收口前的执行状态 */
    private String oldExecutionStatus;

    /** 是否强制执行中重置 */
    private Boolean force;

    /** 重置原因 */
    private String reason;

    /** 结果：SUCCESS/FAILED */
    private String result;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private Instant createdAt;
}
