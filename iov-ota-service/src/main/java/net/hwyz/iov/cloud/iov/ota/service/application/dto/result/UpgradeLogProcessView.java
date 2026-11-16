package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 升级日志上传摘要（CR-015 §3.3 logSummary）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class UpgradeLogProcessView {

    private String logRequestId;
    private Long executionId;
    private String objectKey;
    private Long objectSizeBytes;
    private String logDigest;
    private String digestAlgorithm;
    private String uploadState;
    private String failureReason;
    private Instant authorizedAt;
    private Instant uploadedAt;
    private Instant receivedAt;
    /** 历史兼容字段：不再是永久权威凭证（CR-026） */
    private String logUrl;
    private Instant uploadTime;
}
