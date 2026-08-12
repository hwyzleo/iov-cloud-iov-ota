package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 日志上传授权结果（CR-012 §5.8、US-082）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAuthResult {

    /** 日志上传申请ID */
    private String logRequestId;

    /** 对象存储键 */
    private String objectKey;

    /** 上传凭证 */
    private String credentialToken;

    /** 凭证过期时间 */
    private Instant expiresAt;
}
