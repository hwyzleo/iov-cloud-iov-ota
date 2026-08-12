package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 任务检测结果（CR-012 §5.1、US-074）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {

    /** 清单处置：ACCEPTED/FULL_REQUIRED/REVISION_CONFLICT/DIGEST_MISMATCH/ALGORITHM_UNSUPPORTED */
    private String inventoryDisposition;

    /** 可用性状态：NONE/NOT_RELEASED/AVAILABLE/BLOCKED/PAUSED/CANCELED/SUPERSEDED */
    private String availabilityStatus;

    /** 是否可见 */
    private boolean visible;

    /** 是否允许下载 */
    private boolean downloadAllowed;

    /** 是否允许申请安装 */
    private boolean installRequestAllowed;

    /** 匹配的车辆任务列表 */
    private List<MatchedVehicleTask> matchedTasks;

    /** 匹配的车辆任务信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedVehicleTask {
        private Long vehicleTaskId;
        private Long taskId;
        private Long taskRevision;
        private String snapshotDigest;
        private boolean snapshotChanged;
        private boolean reconsentRequired;
        private String localDisposition;
        private String packageCacheAction;
        private Instant releaseAt;
        private Instant startTime;
        private Instant endTime;
    }
}
