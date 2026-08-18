package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 单 VehicleTask 当前权威授权状态（CR-016 §6）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleTaskConsentCurrentResult {

    private Long vehicleTaskId;
    private String consentState;
    private Long currentConsentId;
    private String currentReceiptId;
    private String scopeDigest;
    private String articleVersion;
    private Instant consentUpdatedAt;
    /** 当前凭据是否有效（统一 ConsentPolicy 判定） */
    private boolean valid;
    /** 有效/无效原因；为 null 表示凭据有效 */
    private String invalidReason;
}
