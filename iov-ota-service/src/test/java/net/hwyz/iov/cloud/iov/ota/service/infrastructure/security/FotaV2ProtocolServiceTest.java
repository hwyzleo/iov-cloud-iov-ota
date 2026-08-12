package net.hwyz.iov.cloud.iov.ota.service.infrastructure.security;

import net.hwyz.iov.cloud.iov.ota.service.common.exception.FotaV2Exception;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.FotaV2ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FotaV2ProtocolService 公共协议校验测试（CR-012 §4、US-073）
 *
 * @author hwyz_leo
 */
@DisplayName("FotaV2ProtocolService 公共协议校验")
class FotaV2ProtocolServiceTest {

    private FotaV2ProtocolService service;

    @BeforeEach
    void setUp() {
        service = new FotaV2ProtocolService();
    }

    @Test
    @DisplayName("协议版本 2.0 通过")
    void validateProtocolVersion_supported() {
        assertDoesNotThrow(() -> service.validateProtocolVersion("2.0"));
    }

    @Test
    @DisplayName("协议版本不支持抛异常")
    void validateProtocolVersion_unsupported_throws() {
        FotaV2Exception e = assertThrows(FotaV2Exception.class,
                () -> service.validateProtocolVersion("1.0"));
        assertEquals(FotaV2ErrorCode.AUTH_PROTOCOL_UNSUPPORTED, e.getErrorCode());
    }

    @Test
    @DisplayName("缺少协议版本抛异常")
    void validateProtocolVersion_missing_throws() {
        assertThrows(FotaV2Exception.class, () -> service.validateProtocolVersion(null));
    }

    @Test
    @DisplayName("设备绑定 VIN + deviceId 通过")
    void validateDeviceBinding_valid() {
        assertDoesNotThrow(() -> service.validateDeviceBinding("VIN001", "DEV001"));
    }

    @Test
    @DisplayName("设备绑定缺失抛异常")
    void validateDeviceBinding_missing_throws() {
        assertThrows(FotaV2Exception.class, () -> service.validateDeviceBinding(null, "DEV001"));
    }

    @Test
    @DisplayName("timestamp + nonce 通过防重放")
    void validateReplayProtection_valid() {
        long now = Instant.now().toEpochMilli();
        assertDoesNotThrow(() -> service.validateReplayProtection(now, UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("时钟偏差超限抛异常")
    void validateReplayProtection_clockDeviation_throws() {
        long past = Instant.now().toEpochMilli() - 10 * 60 * 1000L; // 10 分钟前
        FotaV2Exception e = assertThrows(FotaV2Exception.class,
                () -> service.validateReplayProtection(past, UUID.randomUUID().toString()));
        assertEquals(FotaV2ErrorCode.AUTH_CLOCK_DEVIATION, e.getErrorCode());
    }

    @Test
    @DisplayName("重复 nonce 抛重放异常")
    void validateReplayProtection_duplicateNonce_throws() {
        long now = Instant.now().toEpochMilli();
        String nonce = UUID.randomUUID().toString();
        service.validateReplayProtection(now, nonce);
        FotaV2Exception e = assertThrows(FotaV2Exception.class,
                () -> service.validateReplayProtection(now, nonce));
        assertEquals(FotaV2ErrorCode.AUTH_REPLAY_DETECTED, e.getErrorCode());
    }

    @Test
    @DisplayName("缺少 timestamp 或 nonce 抛异常")
    void validateReplayProtection_missing_throws() {
        assertThrows(FotaV2Exception.class, () -> service.validateReplayProtection(null, null));
    }

    @Test
    @DisplayName("写操作缺幂等键抛异常")
    void validateIdempotencyKey_writeMissing_throws() {
        FotaV2Exception e = assertThrows(FotaV2Exception.class,
                () -> service.validateIdempotencyKey(null, true));
        assertEquals(FotaV2ErrorCode.AUTH_IDEMPOTENCY_MISSING, e.getErrorCode());
    }

    @Test
    @DisplayName("写操作有幂等键通过")
    void validateIdempotencyKey_writePresent_passes() {
        assertDoesNotThrow(() -> service.validateIdempotencyKey("idem-001", true));
    }

    @Test
    @DisplayName("读操作无需幂等键")
    void validateIdempotencyKey_read_passes() {
        assertDoesNotThrow(() -> service.validateIdempotencyKey(null, false));
    }
}
