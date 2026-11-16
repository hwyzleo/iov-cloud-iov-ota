package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 升级日志登记实体（tb_upgrade_log，IOV-OTA-DSN-CR-026 §6.5）。
 *
 * <p>Object 键是事实，`log_url` 不再是永久权威凭证；只有经过对象存在性、大小与摘要核验后
 * 才可置为 {@code UPLOADED}。日志正文不进入本实体、消息或数据库。</p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@Accessors(chain = true)
public class UpgradeLog {

    /** 状态：已授权（未核验） */
    public static final String STATE_AUTHORIZED = "AUTHORIZED";
    /** 状态：核验通过 */
    public static final String STATE_UPLOADED = "UPLOADED";
    /** 状态：确定性失败 */
    public static final String STATE_FAILED = "FAILED";
    /** 状态：授权过期 */
    public static final String STATE_EXPIRED = "EXPIRED";

    private Long id;
    private Long taskId;
    private String vin;
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
    private String logUrl;
    private Instant authorizedAt;
    private Instant uploadedAt;
    private Instant receivedAt;
    private Instant uploadTime;

    public boolean isUploaded() {
        return STATE_UPLOADED.equals(uploadState);
    }

    public boolean isFailed() {
        return STATE_FAILED.equals(uploadState);
    }

    public boolean isAuthorized() {
        return STATE_AUTHORIZED.equals(uploadState);
    }

    /**
     * 标记为已授权。
     */
    public UpgradeLog markAuthorized() {
        this.uploadState = STATE_AUTHORIZED;
        return this;
    }

    /**
     * 标记为核验通过。
     */
    public UpgradeLog markUploaded(Instant uploadedAt) {
        this.uploadState = STATE_UPLOADED;
        this.uploadResult = "SUCCESS";
        this.failureReason = null;
        this.uploadedAt = uploadedAt;
        this.uploadTime = uploadedAt;
        return this;
    }

    /**
     * 标记为确定性失败（保留稳定原因，不删除历史）。
     */
    public UpgradeLog markFailed(String failureReason) {
        this.uploadState = STATE_FAILED;
        this.uploadResult = "FAIL";
        this.failureReason = failureReason;
        return this;
    }
}
