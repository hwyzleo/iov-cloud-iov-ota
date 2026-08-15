package net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PAR-PROTO Release Guard（CR-014 §2.2/§4.18.1）
 *
 * <p>启动时 fail-closed 校验（任一不一致抛异常阻断启动）：
 * <ol>
 *   <li>canonical .proto 源（随制品以 classpath 资源发布）SHA-256 与 Manifest 比对；</li>
 *   <li>payload_type_registry 与 canonical fota .proto message 集合交叉校验（防手写漂移）；</li>
 *   <li>注册表每个 payload_type 的生成类（vehicle.fota.v1.&lt;Outer&gt;$&lt;Name&gt;）可解析；</li>
 *   <li>Envelope 与 GatewayDeliveryStatus 生成类可解析；</li>
 *   <li>protocol major 与注册表 hash 与 Manifest 一致。</li>
 * </ol>
 * 校验通过后建立 {@link PayloadTypeRegistry}，供 FotaPayloadRouter 使用。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParProtoReleaseGuard {

    /** canonical 源资源路径（与发布制品一致，仅用于 hash 与交叉校验） */
    private static final String COMMON_PROTO = "vehicle/common/v1/envelope.proto";
    private static final String FOTA_PREFIX = "vehicle/fota/v1/";
    private static final List<String> FOTA_PROTOS = List.of(
            "vehicle/fota/v1/consent.proto",
            "vehicle/fota/v1/execution.proto",
            "vehicle/fota/v1/log.proto",
            "vehicle/fota/v1/package.proto",
            "vehicle/fota/v1/policy.proto",
            "vehicle/fota/v1/reconcile.proto",
            "vehicle/fota/v1/task.proto",
            "vehicle/fota/v1/types.proto");
    private static final String DELIVERY_PROTO = "vagw/v1/delivery.proto";
    private static final String MANIFEST = "par-proto/manifest.yaml";
    private static final String REGISTRY = "par-proto/payload_type_registry.json";

    private static final String ENVELOPE_CLASS = "vehicle.common.v1.Envelope$VehicleMessageEnvelope";
    private static final String DELIVERY_CLASS = "vagw.v1.Delivery$GatewayDeliveryStatus";
    private static final String FOTA_PACKAGE = "vehicle.fota.v1";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^message\\s+([A-Za-z0-9_]+)",
            Pattern.MULTILINE);

    private final ObjectMapper objectMapper;

    private PayloadTypeRegistry registry;

    @PostConstruct
    public void verify() {
        try {
            Map<String, Object> manifest = loadManifest();
            doVerify(manifest);
            log.info("PAR-PROTO ReleaseGuard 校验通过：release[{}] protocol_major[{}] payloadTypes[{}]",
                    manifest.get("par_proto_release"), registry.getProtocolMajor(), registry.entries().size());
        } catch (Exception e) {
            log.error("PAR-PROTO ReleaseGuard 校验失败（fail-closed，禁止启动）：{}", e.getMessage(), e);
            throw new IllegalStateException("PAR-PROTO release 校验失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用给定 manifest 执行校验（测试可注入篡改的 manifest 验证 fail-closed）。
     *
     * @throws IllegalStateException 任一不一致
     */
    void doVerify(Map<String, Object> manifest) {
        try {
            verifyCanonicalSources(manifest);
            PayloadTypeRegistry built = buildRegistry(manifest);
            this.registry = built;
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            throw new IllegalStateException("PAR-PROTO release 校验失败: " + e.getMessage(), e);
        }
    }

    public PayloadTypeRegistry registry() {
        return registry;
    }

    // ------------------------------------------------------------------ manifest

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadManifest() throws IOException {
        try (InputStream in = requireResource(MANIFEST)) {
            return new Yaml().load(in);
        }
    }

    private void verifyCanonicalSources(Map<String, Object> manifest) throws IOException {
        assertHash(COMMON_PROTO, manifest.get("common_proto_sha256"), "common");
        byte[] fota = concat(FOTA_PROTOS);
        assertHashBytes("aggregated-fota", fota, str(manifest.get("fota_proto_sha256")), "fota");
        assertHash(DELIVERY_PROTO, manifest.get("delivery_proto_sha256"), "delivery");
    }

    private void assertHash(String resource, Object expectedHex, String label) throws IOException {
        try (InputStream in = requireResource(resource)) {
            byte[] bytes = in.readAllBytes();
            assertHashBytes(resource, bytes, str(expectedHex), label);
        }
    }

    private void assertHashBytes(String what, byte[] bytes, String expectedHex, String label) {
        String actual = sha256(bytes);
        if (!actual.equalsIgnoreCase(expectedHex)) {
            throw new IllegalStateException(String.format(
                    "canonical 源 %s SHA-256 漂移：%s 实际[%s] 期望[%s]", label, what, actual, expectedHex));
        }
    }

    // ------------------------------------------------------------------ registry

    private PayloadTypeRegistry buildRegistry(Map<String, Object> manifest) throws IOException {
        byte[] registryBytes;
        try (InputStream in = requireResource(REGISTRY)) {
            registryBytes = in.readAllBytes();
        }
        // 注册表 hash 与 Manifest 比对
        assertHashBytes("registry", registryBytes, str(manifest.get("payload_type_registry_sha256")),
                "payload_type_registry");

        JsonNode root = objectMapper.readTree(new String(registryBytes, StandardCharsets.UTF_8));
        int protocolMajor = root.path("protocol_major").asInt(-1);
        int manifestMajor = manifest.get("protocol_major") == null
                ? -1 : ((Number) manifest.get("protocol_major")).intValue();
        if (protocolMajor != manifestMajor) {
            throw new IllegalStateException("protocol_major 不一致：registry[" + protocolMajor
                    + "] manifest[" + manifestMajor + "]");
        }

        // canonical fota .proto → message 集合（交叉校验，防手写 allowlist 漂移）
        Map<String, String> messageToFile = parseMessageToFile(FOTA_PROTOS);
        verifyGeneratedClasses(builtRegistryCandidate(root, protocolMajor, messageToFile), messageToFile);

        Map<String, PayloadTypeEntry> entries = new LinkedHashMap<>();
        for (JsonNode node : root.path("payloadTypes")) {
            String payloadType = node.path("payloadType").asText();
            String direction = node.path("direction").asText();
            String messageKind = node.path("messageKind").asText();
            String family = node.path("family").asText();
            String response = node.path("responsePayloadType").asText("");
            String name = simpleName(payloadType);
            if (!messageToFile.containsKey(name)) {
                throw new IllegalStateException("注册表 payloadType[" + payloadType
                        + "] 未在 canonical fota .proto 中声明");
            }
            entries.put(payloadType, new PayloadTypeEntry(payloadType, direction, messageKind,
                    family, response.isBlank() ? null : response));
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("payload_type_registry 为空");
        }
        return new PayloadTypeRegistry(root.path("service").asText(FOTA_PACKAGE),
                protocolMajor, entries);
    }

    private PayloadTypeRegistry builtRegistryCandidate(JsonNode root, int protocolMajor,
                                                       Map<String, String> messageToFile) {
        Map<String, PayloadTypeEntry> entries = new LinkedHashMap<>();
        for (JsonNode node : root.path("payloadTypes")) {
            String payloadType = node.path("payloadType").asText();
            String name = simpleName(payloadType);
            if (!messageToFile.containsKey(name)) {
                throw new IllegalStateException("注册表 payloadType[" + payloadType
                        + "] 未在 canonical fota .proto 中声明");
            }
            entries.put(payloadType, new PayloadTypeEntry(payloadType, node.path("direction").asText(),
                    node.path("messageKind").asText(), node.path("family").asText(),
                    node.path("responsePayloadType").asText("")));
        }
        return new PayloadTypeRegistry(root.path("service").asText(FOTA_PACKAGE), protocolMajor, entries);
    }

    private void verifyGeneratedClasses(PayloadTypeRegistry built, Map<String, String> messageToFile) {
        for (PayloadTypeEntry entry : built.entries().values()) {
            resolveClass(fotaClass(entry.payloadType(), messageToFile));
        }
        resolveClass(ENVELOPE_CLASS);
        resolveClass(DELIVERY_CLASS);
    }

    private String fotaClass(String payloadType, Map<String, String> messageToFile) {
        String name = simpleName(payloadType);
        // outer 类名 = 声明该 message 的 proto 文件 stem 首字母大写（java_multiple_files=false）
        String file = messageToFile.get(name);
        if (file == null) {
            throw new IllegalStateException("无法定位 message[" + name + "] 的 proto 文件");
        }
        String stem = file.substring(file.lastIndexOf('/') + 1).replace(".proto", "");
        String outer = Character.toUpperCase(stem.charAt(0)) + stem.substring(1);
        return FOTA_PACKAGE + "." + outer + "$" + name;
    }

    private void resolveClass(String className) {
        try {
            Class.forName(className, false, ParProtoReleaseGuard.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PAR-PROTO 生成类缺失: " + className, e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, String> parseMessageToFile(List<String> protos) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (String path : protos) {
            try (InputStream in = requireResource(path)) {
                String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Matcher m = MESSAGE_PATTERN.matcher(src);
                while (m.find()) {
                    map.putIfAbsent(m.group(1), path);
                }
            }
        }
        return map;
    }

    private byte[] concat(List<String> resources) throws IOException {
        List<byte[]> all = new ArrayList<>();
        int total = 0;
        for (String path : resources) {
            try (InputStream in = requireResource(path)) {
                byte[] b = in.readAllBytes();
                all.add(b);
                total += b.length;
            }
        }
        byte[] out = new byte[total];
        int off = 0;
        for (byte[] b : all) {
            System.arraycopy(b, 0, out, off, b.length);
            off += b.length;
        }
        return out;
    }

    private static InputStream requireResource(String path) {
        InputStream in = ParProtoReleaseGuard.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("classpath 缺少资源: " + path);
        }
        return in;
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String simpleName(String payloadType) {
        int idx = payloadType.lastIndexOf('.');
        return idx >= 0 ? payloadType.substring(idx + 1) : payloadType;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
