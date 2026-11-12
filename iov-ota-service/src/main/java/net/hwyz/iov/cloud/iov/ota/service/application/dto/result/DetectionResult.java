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

    /** 云端已受理的清单 revision（ACCEPTED 时非空，响应必须回显） */
    private Long acceptedInventoryRevision;

    /** 已接受清单的 inventory model（SINGLE_IMAGE/MULTI_TARGET） */
    private String inventoryModel;

    /** 已接受清单的 canonicalization 版本 */
    private Integer canonicalizationVersion;

    /** 已接受清单的 canonical digest（小写 hex，v2 时非空） */
    private String canonicalDigestHex;

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
        /** 活动ID（TaskCheckResponse.task.activity_id 投影） */
        private Long activityId;
        /** 安装计划版本（TaskCheckResponse.task.install_plan_version 投影；重试间必须稳定） */
        private String installPlanVersion;
        /** 任务包清单（TaskCheckResponse.task.package_list 投影） */
        private List<MatchedPackage> packages;
    }

    /**
     * 匹配任务中的单个升级包（对应 vehicle.fota.v1.PackageSummary）。
     *
     * <p>包清单是车端下载/校验/安装的唯一依据：为空时车端无法申请下载凭证，
     * 云端不得再返回 PROCEED。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedPackage {
        private String packageId;
        private String packageRevision;
        private String planNodeId;
        private String ecuId;
        private String componentId;
        private String sourceVersion;
        private String targetVersion;
        private Long sizeBytes;
        private String digestAlgorithm;
        private String digestHex;
        private String differentialType;
        private Boolean encrypted;
    }
}
