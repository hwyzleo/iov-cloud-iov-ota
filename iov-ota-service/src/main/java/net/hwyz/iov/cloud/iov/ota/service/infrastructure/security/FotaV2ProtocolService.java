package net.hwyz.iov.cloud.iov.ota.service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.FotaV2Exception;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.FotaV2ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CCP FOTA v2 公共协议校验服务（CR-012 §4、US-073）
 *
 * <p>统一拦截链：设备绑定校验、timestamp + nonce 防重放、时钟偏差、protocolVersion 校验。
 *
 * <p>当前设备证书/Token 校验为占位实现；防重放使用进程内缓存（生产需换 Redis）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class FotaV2ProtocolService {

    /** 允许的最大时钟偏差（毫秒） */
    private static final long MAX_CLOCK_DEVIATION_MS = 5 * 60 * 1000L;
    /** 支持的协议版本 */
    private static final String SUPPORTED_PROTOCOL_VERSION = "2.0";
    /** 防重放缓存：nonce -> 时间戳 */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();
    /** nonce 缓存有效期（毫秒） */
    private static final long NONCE_TTL_MS = 10 * 60 * 1000L;

    /**
     * 校验协议版本。
     */
    public void validateProtocolVersion(String protocolVersion) {
        if (protocolVersion == null || protocolVersion.isBlank()) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_PROTOCOL_UNSUPPORTED, "缺少协议版本");
        }
        if (!SUPPORTED_PROTOCOL_VERSION.equals(protocolVersion)) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_PROTOCOL_UNSUPPORTED,
                    "不支持的协议版本: " + protocolVersion);
        }
    }

    /**
     * 校验设备绑定（VIN + deviceId）。
     *
     * <p>TODO: 接入设备证书/Token 校验，验证 Token 与 VIN、deviceId 绑定关系。
     */
    public void validateDeviceBinding(String vin, String deviceId) {
        if (vin == null || vin.isBlank() || deviceId == null || deviceId.isBlank()) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_DEVICE_BIND_FAIL, "VIN 或 deviceId 缺失");
        }
        // TODO: 校验设备证书/Token 与 VIN、deviceId 绑定
    }

    /**
     * 校验 timestamp + nonce 防重放和时钟偏差。
     */
    public void validateReplayProtection(Long timestamp, String nonce) {
        if (timestamp == null || nonce == null || nonce.isBlank()) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_REPLAY_DETECTED, "timestamp 或 nonce 缺失");
        }
        long now = Instant.now().toEpochMilli();
        long deviation = Math.abs(now - timestamp);
        if (deviation > MAX_CLOCK_DEVIATION_MS) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_CLOCK_DEVIATION,
                    "时钟偏差超限: " + deviation + "ms");
        }
        Long previous = nonceCache.putIfAbsent(nonce, timestamp);
        if (previous != null) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_REPLAY_DETECTED, "检测到重复 nonce");
        }
        // 清理过期 nonce
        nonceCache.entrySet().removeIf(e -> now - e.getValue() > NONCE_TTL_MS);
    }

    /**
     * 校验写操作幂等键存在。
     */
    public void validateIdempotencyKey(String idempotencyKey, boolean writeOperation) {
        if (writeOperation && (idempotencyKey == null || idempotencyKey.isBlank())) {
            throw new FotaV2Exception(FotaV2ErrorCode.AUTH_IDEMPOTENCY_MISSING, "写操作缺少幂等键");
        }
    }
}
