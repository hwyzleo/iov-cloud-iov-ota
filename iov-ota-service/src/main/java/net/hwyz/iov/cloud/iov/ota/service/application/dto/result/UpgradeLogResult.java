package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 升级日志登记（CR-015 §3.3 upgradeLogs 子资源）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class UpgradeLogResult {

    private Long taskId;
    private String vinMasked;
    private Long vehicleTaskId;
    private Long executionId;
    private String logRequestId;
    private String logScope;
    private String desensitizeVersion;
    private String privacyLevel;
    private String objectKey;
    private String logObjectId;
    private Long objectSizeBytes;
    private String logDigest;
    private String digestAlgorithm;
    private String uploadState;
    private String uploadResult;
    private String failureReason;
    /** 历史兼容字段：不再是永久权威凭证（CR-026） */
    private String logUrl;
    private Instant authorizedAt;
    private Instant uploadedAt;
    private Instant receivedAt;
    private Instant uploadTime;
}
