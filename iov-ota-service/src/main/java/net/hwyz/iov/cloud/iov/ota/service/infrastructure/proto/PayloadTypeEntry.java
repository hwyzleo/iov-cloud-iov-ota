package net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto;

/**
 * PAR-PROTO PayloadType 注册表条目（CR-014 §5）
 *
 * <p>每个 vehicle.fota.v1.&lt;MessageName&gt; 的边界语义：方向、合法 MessageKind、业务族与
 * 关联响应类型。来源为 par-proto/payload_type_registry.json（与 canonical .proto 交叉校验）。
 *
 * @author hwyz_leo
 */
public record PayloadTypeEntry(
        String payloadType,
        String direction,        // INBOUND / OUTBOUND
        String messageKind,      // REQUEST / RESPONSE / EVENT
        String family,           // 业务族，如 TASK_CHECK
        String responsePayloadType) {

    public boolean isInbound() {
        return "INBOUND".equals(direction);
    }

    public boolean isOutbound() {
        return "OUTBOUND".equals(direction);
    }
}
