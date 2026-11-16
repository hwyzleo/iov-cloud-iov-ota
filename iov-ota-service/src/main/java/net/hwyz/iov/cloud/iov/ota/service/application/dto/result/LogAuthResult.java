package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

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

    /** 日志上传申请ID（EDD-OSS requestId） */
    private String logRequestId;

    /** EDD-OSS 日志对象ID */
    private String objectId;

    /** 对象存储键（服务端生成） */
    private String objectKey;

    /** 预签名 PUT 地址（仅响应出现，不落库/日志） */
    private String uploadUrl;

    /** 上传时必须原样携带的强制头（Content-Type / x-cos-meta-sha256） */
    private Map<String, String> headers;

    /** 单对象上限 */
    private Long maxSizeBytes;

    /** 过期时间 */
    private Instant expiresAt;
}
