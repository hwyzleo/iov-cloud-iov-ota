package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.TraceContext;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * FOTA 出站 Envelope 工厂（CR-014 §6）
 *
 * <p>使用 common generated builder 构造完整 VehicleMessageEnvelope 并序列化冻结 bytes：
 * service=vehicle.fota、全限定 payload_type、正确 message_kind/protocol_version、VIN/device_id、
 * message_id 与关联链。Outbox 首次创建即冻结 bytes，重试不得重建。
 *
 * <p>Envelope 关联规则（§6.3）：
 * <ul>
 *   <li>RESPONSE：继承请求 request_id，correlation_id 指向请求 message_id；</li>
 *   <li>EVENT：使用稳定 message_id，按业务需要带 vehicle_task_id/execution_id；</li>
 *   <li>REQUEST：使用新 message_id 与稳定 idempotency_key。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class FotaEnvelopeFactory {

    private final ParProtoReleaseGuard releaseGuard;

    /**
     * 构造 RESPONSE Envelope（响应请求消息）。
     *
     * @param requestMeta   请求元数据（响应继承 request_id、vin/device、vehicle_task_id/execution_id）
     * @param responsePayload 强类型响应 payload（vehicle.fota.v1.* 生成类）
     */
    public FotaOutboundEnvelope response(FotaMessageMetadata requestMeta, Message responsePayload,
                                         String aggregateType, String aggregateId) {
        String messageId = UUID.randomUUID().toString();
        VehicleMessageEnvelope envelope = VehicleMessageEnvelope.newBuilder()
                .setRequestId(requestMeta.requestId())
                .setTimestampMs(Instant.now().toEpochMilli())
                .setProtocolVersion(protocolVersion())
                .setDeviceId(requestMeta.deviceId())
                .setVin(requestMeta.vin())
                .setVehicleTaskId(orEmpty(requestMeta.vehicleTaskId()))
                .setExecutionId(orEmpty(requestMeta.executionId()))
                .setIdempotencyKey(orEmpty(requestMeta.idempotencyKey()))
                .setPayloadType(responsePayload.getDescriptorForType().getFullName())
                .setPayload(responsePayload.toByteString())
                .setMessageId(messageId)
                .setCorrelationId(requestMeta.messageId())
                .setMessageKind(MessageKind.MESSAGE_KIND_RESPONSE)
                .setService(FotaEnvelopeValidator.FOTA_SERVICE)
                .build();
        return toOutbound(envelope, aggregateType, aggregateId);
    }

    /**
     * 构造 EVENT Envelope（云端主动下发的单向事件，如 ControlCommand）。
     */
    public FotaOutboundEnvelope event(String vin, String deviceId, String requestId,
                                      String vehicleTaskId, String executionId, String idempotencyKey,
                                      Message eventPayload, String aggregateType, String aggregateId) {
        String messageId = UUID.randomUUID().toString();
        VehicleMessageEnvelope envelope = VehicleMessageEnvelope.newBuilder()
                .setRequestId(requestId == null ? "" : requestId)
                .setTimestampMs(Instant.now().toEpochMilli())
                .setProtocolVersion(protocolVersion())
                .setDeviceId(deviceId == null ? "" : deviceId)
                .setVin(vin)
                .setVehicleTaskId(orEmpty(vehicleTaskId))
                .setExecutionId(orEmpty(executionId))
                .setIdempotencyKey(orEmpty(idempotencyKey))
                .setPayloadType(eventPayload.getDescriptorForType().getFullName())
                .setPayload(eventPayload.toByteString())
                .setMessageId(messageId)
                .setMessageKind(MessageKind.MESSAGE_KIND_EVENT)
                .setService(FotaEnvelopeValidator.FOTA_SERVICE)
                .build();
        return toOutbound(envelope, aggregateType, aggregateId);
    }

    private FotaOutboundEnvelope toOutbound(VehicleMessageEnvelope envelope,
                                            String aggregateType, String aggregateId) {
        byte[] bytes = envelope.toByteArray();
        return new FotaOutboundEnvelope(
                envelope.getMessageId(),
                envelope.getPayloadType(),
                normalizeKind(envelope.getMessageKind()),
                envelope.hasCorrelationId() ? envelope.getCorrelationId() : null,
                envelope.getVin(),
                bytes,
                FotaDigests.sha256(bytes),
                aggregateType,
                aggregateId);
    }

    /** proto 枚举名（MESSAGE_KIND_RESPONSE）→ 短名（RESPONSE），与 registry/Outbox 对齐 */
    private static String normalizeKind(MessageKind kind) {
        if (kind == null) {
            return null;
        }
        String name = kind.name();
        return name.startsWith("MESSAGE_KIND_") ? name.substring("MESSAGE_KIND_".length()) : name;
    }

    private String protocolVersion() {
        return releaseGuard.registry().getProtocolVersion();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
