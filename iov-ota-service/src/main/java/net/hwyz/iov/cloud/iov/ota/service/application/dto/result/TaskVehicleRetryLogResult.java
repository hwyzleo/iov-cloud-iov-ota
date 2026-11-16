package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 车辆任务重试/续传轨迹（CR-015 §3.3 retryLogs 子资源）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskVehicleRetryLogResult {

    private Long taskId;
    private String vinMasked;
    private String stage;
    /** 动作：RETRY/RESUME/RESET_OFFSET/EXECUTION_CREATED（CR-026） */
    private String action;
    private Integer attemptNo;
    private Long offset;
    private String result;
    private String reason;
    /** 车端错误码（CR-026） */
    private String errorCode;
    private Instant retriedAt;
    /** 车端发生时间（未知为 null，CR-026） */
    private Instant occurredAt;
    /** 云端受理时间（CR-026） */
    private Instant receivedAt;
}
