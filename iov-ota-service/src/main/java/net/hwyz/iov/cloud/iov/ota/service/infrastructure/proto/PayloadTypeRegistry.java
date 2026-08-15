package net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto;

import java.util.Collections;
import java.util.Map;

/**
 * PAR-PROTO PayloadType 注册表（CR-014 §5）
 *
 * <p>Router 的唯一输入。由 ParProtoReleaseGuard 在启动时从
 * par-proto/payload_type_registry.json 加载并与 canonical .proto 交叉校验后建立。
 *
 * @author hwyz_leo
 */
public final class PayloadTypeRegistry {

    private final String service;
    private final int protocolMajor;
    private final Map<String, PayloadTypeEntry> entriesByPayloadType;

    public PayloadTypeRegistry(String service, int protocolMajor,
                               Map<String, PayloadTypeEntry> entriesByPayloadType) {
        this.service = service;
        this.protocolMajor = protocolMajor;
        this.entriesByPayloadType = Collections.unmodifiableMap(entriesByPayloadType);
    }

    public String getService() {
        return service;
    }

    public int getProtocolMajor() {
        return protocolMajor;
    }

    /** 按全限定 payload_type 解析条目；未知类型返回 null（调用方 fail-closed）。 */
    public PayloadTypeEntry resolve(String payloadType) {
        return entriesByPayloadType.get(payloadType);
    }

    public Map<String, PayloadTypeEntry> entries() {
        return entriesByPayloadType;
    }

    public boolean contains(String payloadType) {
        return entriesByPayloadType.containsKey(payloadType);
    }
}
