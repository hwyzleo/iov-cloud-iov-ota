package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SnapshotDigest 摘要比较测试。
 *
 * <p>覆盖 2026-11-15 实车“包清单摘要校验失败”问题引入的归一化比较语义：
 * 十六进制大小写、算法前缀、空白差异不得被判定为摘要不一致。
 *
 * @author hwyz_leo
 */
@DisplayName("SnapshotDigest - 摘要一致性比较")
class SnapshotDigestTest {

    @Test
    @DisplayName("完全相同 -> 一致")
    void matches_identical() {
        assertTrue(SnapshotDigest.of("a1b2c3").matches(SnapshotDigest.of("a1b2c3")));
    }

    @Test
    @DisplayName("十六进制大小写不同 -> 仍一致")
    void matches_hexCaseInsensitive() {
        assertTrue(SnapshotDigest.of("A1B2C3").matches(SnapshotDigest.of("a1b2c3")));
    }

    @Test
    @DisplayName("算法前缀 / 0x 前缀 / 空白差异 -> 仍一致")
    void matches_prefixAndWhitespaceNormalized() {
        assertTrue(SnapshotDigest.of(" sha-256:A1B2C3 ").matches(SnapshotDigest.of("0xa1b2c3")));
        assertTrue(SnapshotDigest.of("SHA-256:A1B2C3").matches(SnapshotDigest.of("a1b2c3")));
    }

    @Test
    @DisplayName("值不同 -> 不一致")
    void matches_differentValue() {
        assertFalse(SnapshotDigest.of("a1b2c3").matches(SnapshotDigest.of("a1b2c4")));
    }

    @Test
    @DisplayName("对方为 null（云端未冻结）-> 不能证明一致")
    void matches_nullOther() {
        assertFalse(SnapshotDigest.of("a1b2c3").matches(null));
    }
}
