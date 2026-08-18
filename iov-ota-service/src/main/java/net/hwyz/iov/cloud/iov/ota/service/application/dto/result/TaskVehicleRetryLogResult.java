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
    private Integer attemptNo;
    private Long offset;
    private String result;
    private String reason;
    private Instant retriedAt;
}
