package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 包清单摘要规范化算法测试（CR-024 §4.28.2 canonicalization v1）。
 *
 * <p>重点：确定性（入参顺序无关）、对内容变化敏感、空值与空串区分、规范化版本隔离。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("PackageManifestDigestService - 包清单摘要规范化")
class PackageManifestDigestServiceTest {

    private final PackageManifestDigestService service = new PackageManifestDigestService();

    private DetectionResult.MatchedPackage pkg(String packageId, String revision, String targetVersion, String digestHex) {
        return DetectionResult.MatchedPackage.builder()
                .packageId(packageId)
                .packageRevision(revision)
                .planNodeId("NODE-1")
                .ecuId("CCU")
                .componentId("CCU_FOTA")
                .sourceVersion("V1.0.0")
                .targetVersion(targetVersion)
                .sizeBytes(1024L)
                .digestAlgorithm("sha-256")
                .digestHex(digestHex)
                .differentialType("")
                .encrypted(Boolean.FALSE)
                .build();
    }

    @Test
    @DisplayName("确定性：入参顺序不同但内容相同 -> 摘要一致")
    void digest_orderIndependent() {
        DetectionResult.MatchedPackage a = pkg("PKG-A", "V1", "V2", "aa11");
        DetectionResult.MatchedPackage b = pkg("PKG-B", "V1", "V2", "bb22");

        String d1 = service.digest(List.of(a, b), "task-1-r1", 1L);
        String d2 = service.digest(List.of(b, a), "task-1-r1", 1L);

        assertEquals(d1, d2);
        assertEquals(64, d1.length());
        assertTrue(d1.matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("敏感性：目标版本 / 摘要 / size 变化 -> 摘要变化")
    void digest_sensitiveToContent() {
        String base = service.digest(List.of(pkg("PKG-A", "V1", "V2", "aa11")), "task-1-r1", 1L);

        assertNotEquals(base, service.digest(List.of(pkg("PKG-A", "V1", "V3", "aa11")), "task-1-r1", 1L));
        assertNotEquals(base, service.digest(List.of(pkg("PKG-A", "V1", "V2", "aa12")), "task-1-r1", 1L));
        assertNotEquals(base, service.digest(List.of(pkg("PKG-A", "V2", "V2", "aa11")), "task-1-r1", 1L));
        assertNotEquals(base, service.digest(List.of(pkg("PKG-A", "V1", "V2", "aa11")), "task-1-r2", 1L));
    }

    @Test
    @DisplayName("排除 sourceVersion：清单当前版本变化不影响摘要")
    void digest_excludesSourceVersion() {
        DetectionResult.MatchedPackage a = pkg("PKG-A", "V1", "V2", "aa11");
        DetectionResult.MatchedPackage b = pkg("PKG-A", "V1", "V2", "aa11");
        b.setSourceVersion("V9.9.9");

        assertEquals(service.digest(List.of(a), "task-1-r1", 1L),
                service.digest(List.of(b), "task-1-r1", 1L));
    }

    @Test
    @DisplayName("空值 vs 空串：两者编码不同，摘要不同")
    void digest_nullVsEmptyDistinguished() {
        DetectionResult.MatchedPackage nullPlanNode = pkg("PKG-A", "V1", "V2", "aa11");
        nullPlanNode.setPlanNodeId(null);
        DetectionResult.MatchedPackage emptyPlanNode = pkg("PKG-A", "V1", "V2", "aa11");
        emptyPlanNode.setPlanNodeId("");

        assertNotEquals(service.digest(List.of(nullPlanNode), "task-1-r1", 1L),
                service.digest(List.of(emptyPlanNode), "task-1-r1", 1L));
    }

    @Test
    @DisplayName("空清单仍产出稳定摘要（不抛异常）")
    void digest_emptyList() {
        String d1 = service.digest(Collections.emptyList(), "task-1-r1", 1L);
        String d2 = service.digest(new ArrayList<>(), "task-1-r1", 1L);

        assertEquals(d1, d2);
        assertEquals(64, d1.length());
    }

    @Test
    @DisplayName("密封值：摘要 + 规范化版本 + 可读快照内容一致")
    void seal_containsDigestVersionAndContent() {
        VehicleTaskSnapshotSeal seal = service.seal(List.of(pkg("PKG-A", "V1", "V2", "aa11")), "task-1-r1", 1L);

        assertEquals(PackageManifestDigestService.CANONICALIZATION_VERSION, seal.getCanonicalizationVersion());
        assertEquals(service.digest(List.of(pkg("PKG-A", "V1", "V2", "aa11")), "task-1-r1", 1L),
                seal.getSnapshotDigest());
        assertTrue(seal.getSnapshotContent().contains("\"packageId\":\"PKG-A\""));
        assertTrue(seal.getSnapshotContent().contains("\"canonicalizationVersion\":1"));
    }
}
