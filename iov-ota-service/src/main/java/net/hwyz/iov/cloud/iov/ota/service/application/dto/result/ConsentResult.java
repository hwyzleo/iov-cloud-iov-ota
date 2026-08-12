package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户授权结果（CR-012 §5.3、US-077）
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

    /** 授权状态 */
    private String consentState;

    /** 是否已接受 */
    private boolean accepted;

    /** 有效授权状态（与 accepted 分离） */
    private String effectiveConsentState;

    /** 是否需重新授权 */
    private boolean reconsentRequired;
}
