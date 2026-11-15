package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 车辆任务/任务/活动基础信息（CR-015 §3.3 vehicleTask）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskVehicleProcessView {

    private Long taskId;
    private String taskName;
    private Long activityId;
    private String activityName;
    private Long taskRevision;
    private String snapshotDigest;
    private String vehicleTaskStatus;
    private String availabilityStatus;
    private String downloadReadyState;
    private String consentState;
    private Instant releaseAt;
    private Instant startTime;
    private Instant endTime;
    private String localDisposition;
    private String packageCacheAction;
    private Integer lastAttemptNo;
    private Long activeExecutionId;
    private Integer downloadRetryCount;
    private Integer installRetryCount;
    private String lastFailReason;

    /** 取代者车辆任务ID（IOV-OTA-DSN-CR-025 §4.29.7；仅被取代的历史行非空） */
    private Long supersededBy;

    /** 是否当前车辆任务（superseded_by IS NULL） */
    private Boolean isCurrent;
}
