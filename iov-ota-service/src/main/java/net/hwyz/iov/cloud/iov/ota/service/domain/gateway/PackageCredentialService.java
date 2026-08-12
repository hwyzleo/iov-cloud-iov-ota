package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

import java.time.Duration;
import java.time.Instant;

/**
 * 包下载凭证签发网关（CR-012 §5.4）
 *
 * <p>TODO: 接入 OSS/CDN 进行预签名凭证签发。当前为本地占位实现。
 * 预签名凭证只在 DOWNLOAD/RESUME/RESET_OFFSET 时返回。
 * offset 定义为对象存储中加密/压缩对象的原始字节位置。
 *
 * @author hwyz_leo
 */
public interface PackageCredentialService {

    /**
     * 签发包下载预签名凭证。
     *
     * @param packageId       包ID
     * @param packageRevision 包版本号
     * @param etag            对象 ETag
     * @param offset          续传偏移量（字节），0 表示从头下载
     * @param ttl             凭证有效期
     * @return 预签名下载凭证
     */
    PackageCredential signDownloadCredential(String packageId, String packageRevision, String etag,
                                             long offset, Duration ttl);

    /**
     * 包下载凭证。
     */
    record PackageCredential(String presignedUrl, String credentialToken, Instant expiresAt, long offset,
                             boolean resetOffset) {
    }
}
