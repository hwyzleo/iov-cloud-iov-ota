package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 单 VehicleTask 的不可变授权历史记录（CR-016 §6）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleTaskConsentResult {

    private Long consentRecordId;
    private String consentResult;
    private String consentReceiptId;
    private Long taskRevision;
    private Long articleId;
    private String articleVersion;
    private String articleHash;
    private String scopeDigest;
    private String channel;
    private String subjectRef;
    private Instant reportedAt;
    private Instant receivedAt;
    private Instant expireAt;
    private Long supersedesConsentId;
    private String sourceModel;
}
