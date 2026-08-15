package net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PAR-PROTO ReleaseGuard 单元测试（CR-014 §2.2/§4.18.1、验收 8）
 *
 * <p>验证：canonical .proto 源 SHA-256 直接比对 Manifest 通过；protocol_major 一致；
 * 注册表与 canonical fota .proto 交叉校验；生成类可解析；任一漂移 fail-closed。
 *
 * @author hwyz_leo
 */
@DisplayName("ParProtoReleaseGuard - PAR-PROTO release fail-closed 校验")
class ParProtoReleaseGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("真实资源下 SHA-256 比对通过并建立注册表")
    void verify_passes_with_real_resources() {
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(objectMapper);
        guard.verify();
        PayloadTypeRegistry registry = guard.registry();
        assertNotNull(registry, "registry 必须建立");
        assertEquals(1, registry.getProtocolMajor());
        assertEquals("vehicle.fota", registry.getService());
        assertEquals(25, registry.entries().size());
        // 12 个 INBOUND 类型必须全部注册（router 依赖）
        for (String inbound : new String[]{
                "vehicle.fota.v1.TaskCheckRequest",
                "vehicle.fota.v1.ConsentReport",
                "vehicle.fota.v1.DownloadGrantRequest",
                "vehicle.fota.v1.StageResultReport",
                "vehicle.fota.v1.InstallPermitRequest",
                "vehicle.fota.v1.ExecutionEvent",
                "vehicle.fota.v1.ControlAckReport",
                "vehicle.fota.v1.FinalResultReport",
                "vehicle.fota.v1.LogGrantRequest",
                "vehicle.fota.v1.LogUploadResult",
                "vehicle.fota.v1.ReconcileRequest",
                "vehicle.fota.v1.PolicyRequest"}) {
            PayloadTypeEntry entry = registry.resolve(inbound);
            assertNotNull(entry, "注册表缺少 INBOUND: " + inbound);
            assertTrue(entry.isInbound(), "应标记 INBOUND: " + inbound);
        }
    }

    @Test
    @DisplayName("canonical 源 SHA-256 漂移 → fail-closed")
    void verify_fails_closed_on_source_drift() {
        Map<String, Object> manifest = loadManifest();
        manifest.put("fota_proto_sha256", "0000000000000000000000000000000000000000000000000000000000000000");
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(objectMapper);
        assertThrows(IllegalStateException.class, () -> guard.doVerify(manifest));
    }

    @Test
    @DisplayName("protocol_major 不一致 → fail-closed")
    void verify_fails_closed_on_protocol_major_mismatch() {
        Map<String, Object> manifest = loadManifest();
        manifest.put("protocol_major", 2);
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(objectMapper);
        assertThrows(IllegalStateException.class, () -> guard.doVerify(manifest));
    }

    @Test
    @DisplayName("registry hash 与 Manifest 不一致 → fail-closed")
    void verify_fails_closed_on_registry_hash_mismatch() {
        Map<String, Object> manifest = loadManifest();
        manifest.put("payload_type_registry_sha256",
                "1111111111111111111111111111111111111111111111111111111111111111");
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(objectMapper);
        assertThrows(IllegalStateException.class, () -> guard.doVerify(manifest));
    }

    private Map<String, Object> loadManifest() {
        try (var in = getClass().getClassLoader().getResourceAsStream("par-proto/manifest.yaml")) {
            assertNotNull(in, "manifest.yaml 缺失");
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
