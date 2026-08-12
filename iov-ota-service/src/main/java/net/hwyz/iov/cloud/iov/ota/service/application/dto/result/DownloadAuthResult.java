package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 下载授权结果（CR-012 §5.4、US-078）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadAuthResult {

    /** 预签名下载 URL */
    private String presignedUrl;

    /** 凭证令牌 */
    private String credentialToken;

    /** 凭证过期时间 */
    private Instant expiresAt;

    /** 实际下载偏移量 */
    private long offset;

    /** 是否需要重置偏移 */
    private boolean resetOffset;

    /** 包版本号 */
    private String packageRevision;
}
