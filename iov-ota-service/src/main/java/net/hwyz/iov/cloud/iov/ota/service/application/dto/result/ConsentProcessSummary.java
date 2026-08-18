package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 用户授权摘要（CR-015 §3.3 consentSummary）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ConsentProcessSummary {

    private String consentState;
    private String effectiveState;
    private String receiptId;
    private Long termsId;
    private String termsHash;
    private Boolean accepted;
    private Instant revokedTime;
    private Boolean reconsentRequired;
}
