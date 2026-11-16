package net.hwyz.iov.cloud.iov.ota.service.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 软件包元数据完整性格门测试（IOV-OTA-DSN-CR-026 §7.5 / US-020）。
 *
 * @author hwyz_leo
 */
@DisplayName("PackageMetadataGate - 包元数据 fail-closed")
class PackageMetadataGateTest {

    private static final String SHA256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("完整元数据通过")
    void completeMetadata_passes() {
        assertTrue(PackageMetadataGate.incompleteReasons(1024L, SHA256, "sig", "SHA256withECDSA",
                "cert-1", "ota/pkg/1").isEmpty());
    }

    @Test
    @DisplayName("NULL/0/负数大小一律拒绝，不转换成 0")
    void invalidSize_rejected() {
        assertEquals(List.of(PackageMetadataGate.REASON_SIZE),
                PackageMetadataGate.incompleteReasons(null, SHA256, "sig", "alg", "cert", "url"));
        assertEquals(List.of(PackageMetadataGate.REASON_SIZE),
                PackageMetadataGate.incompleteReasons(0L, SHA256, "sig", "alg", "cert", "url"));
        assertEquals(List.of(PackageMetadataGate.REASON_SIZE),
                PackageMetadataGate.incompleteReasons(-1L, SHA256, "sig", "alg", "cert", "url"));
    }

    @Test
    @DisplayName("摘要非 64 位十六进制拒绝；大小写按小写归一化")
    void invalidSha256_rejected() {
        assertTrue(PackageMetadataGate.incompleteReasons(1024L, "ABC", "sig", "alg", "cert", "url")
                .contains(PackageMetadataGate.REASON_SHA256));
        assertTrue(PackageMetadataGate.incompleteReasons(1024L, null, "sig", "alg", "cert", "url")
                .contains(PackageMetadataGate.REASON_SHA256));
        assertTrue(PackageMetadataGate.incompleteReasons(1024L, SHA256 + "f", "sig", "alg", "cert", "url")
                .contains(PackageMetadataGate.REASON_SHA256));
        // 大写摘要按小写归一化后合法（云端统一按小写解释）
        assertTrue(PackageMetadataGate.incompleteReasons(1024L, SHA256.toUpperCase(), "sig", "alg", "cert", "url")
                .isEmpty());
    }

    @Test
    @DisplayName("签名/算法/证书/物理对象缺失分别给出稳定原因")
    void missingSignatureAndObject_rejected() {
        List<String> reasons = PackageMetadataGate.incompleteReasons(1024L, SHA256, " ", null, "", null);
        assertTrue(reasons.contains(PackageMetadataGate.REASON_SIGNATURE));
        assertTrue(reasons.contains(PackageMetadataGate.REASON_SIGN_ALGO));
        assertTrue(reasons.contains(PackageMetadataGate.REASON_SIGNER_CERT));
        assertTrue(reasons.contains(PackageMetadataGate.REASON_OBJECT));
        assertFalse(PackageMetadataGate.isComplete(1024L, SHA256, null, null, null, null));
    }

    @Test
    @DisplayName("原因摘要为空表示完整，超长截断")
    void summary_behaviour() {
        assertNull(PackageMetadataGate.summary(List.of()));
        assertNull(PackageMetadataGate.summary(null));
        String longSummary = PackageMetadataGate.summary(java.util.List.of(
                "REASON_A_REALLY_LONG_NAME_FOR_TESTING", "REASON_B_REALLY_LONG_NAME_FOR_TESTING",
                "REASON_C_REALLY_LONG_NAME_FOR_TESTING", "REASON_D_REALLY_LONG_NAME_FOR_TESTING",
                "REASON_E_REALLY_LONG_NAME_FOR_TESTING", "REASON_F_REALLY_LONG_NAME_FOR_TESTING",
                "REASON_G_REALLY_LONG_NAME_FOR_TESTING"));
        assertEquals(255, longSummary.length());
    }
}
