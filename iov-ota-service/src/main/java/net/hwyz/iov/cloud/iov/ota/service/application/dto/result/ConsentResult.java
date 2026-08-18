package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户授权结果（CR-016 §5、US-102～105）
 *
 * <p>accepted 表示上报被正常校验和记录；effectiveConsentStatus 为当前有效授权状态。
 * 同 messageId/idempotencyKey 重放返回原响应。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResult {

    /** 授权回执ID */
    private String consentReceiptId;

    /** 授权状态（记录结果） */
    private String consentState;

    /** 是否已接受（业务结果） */
    private boolean accepted;

    /** 有效授权状态（与 accepted 分离） */
    private String effectiveConsentState;

    /** 是否需重新授权 */
    private boolean reconsentRequired;

    /** 授权历史记录ID */
    private Long consentRecordId;

    /** 当前授权状态 */
    private String currentConsentState;

    /** 是否幂等重放（同 messageId/idempotencyKey 同摘要复用原响应） */
    private boolean replayed;

    /** 业务错误码（如同键异参冲突 OTA-IDEMPOTENCY-CONFLICT） */
    private String errorCode;

    /** 业务错误信息 */
    private String errorMessage;
}
