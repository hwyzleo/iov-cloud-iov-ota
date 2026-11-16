package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

import java.time.Instant;
import java.util.Map;

/**
 * 日志对象存储网关（IOV-OTA-DSN-CR-026 §7.3）。
 *
 * <p>IOV-OTA 只做薄封装：对象存储授权、真实对象 HEAD 核验与受控下载地址统一由
 * EDD-OSS 提供；本服务不持有对象存储密钥、不直连 Bucket、不自行生成预签名 URL。
 *
 * @author hwyz_leo
 */
public interface LogObjectStorageGateway {

    /**
     * 申请日志对象上传授权（调用方幂等：同一 requestId 返回同一 objectId/objectKey）。
     *
     * @param request 授权申请
     * @return 授权结果（含上传地址与强制头）
     */
    Authorization authorize(AuthorizationRequest request);

    /**
     * 完成上传核验：只有对象存在且大小/摘要一致时返回 UPLOADED 事实。
     *
     * @param objectId 对象标识
     * @return 登记事实
     */
    Fact complete(String objectId);

    /**
     * 查询登记事实（不触发核验）。
     *
     * @param objectId 对象标识
     * @return 登记事实
     */
    Fact detail(String objectId);

    /**
     * 申请短期受控下载地址（仅 UPLOADED 且属于当前调用方）。
     *
     * @param objectId 对象标识
     * @return 短期下载地址事实
     */
    DownloadUrl downloadUrl(String objectId);

    /**
     * 授权申请。
     */
    record AuthorizationRequest(String businessType, String businessId, String businessSubId,
                                String requestId, String fileName, String contentType, long sizeBytes,
                                String sha256Hex, String privacyLevel, Integer ttlSeconds) {
    }

    /**
     * 授权结果。
     */
    record Authorization(String objectId, String objectKey, String uploadUrl, Map<String, String> headers,
                         Instant expiresAt, Long maxSizeBytes) {
    }

    /**
     * 对象登记事实（声明值与 HEAD 观察值分离）。
     */
    record Fact(String objectId, String objectKey, String uploadState, Long sizeBytes, Long actualSizeBytes,
                String sha256Hex, String actualSha256Hex, String etag, String failureReason,
                Instant authorizedAt, Instant uploadedAt, Instant receivedAt) {
    }

    /**
     * 短期下载地址。
     */
    record DownloadUrl(String objectId, String downloadUrl, Instant expiresAt) {
    }
}
