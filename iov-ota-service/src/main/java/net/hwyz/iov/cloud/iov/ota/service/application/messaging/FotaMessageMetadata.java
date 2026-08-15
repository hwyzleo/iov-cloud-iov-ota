package net.hwyz.iov.cloud.iov.ota.service.application.messaging;

import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.TraceContext;

/**
 * FOTA 消息不可变元数据（CR-014 §4.3）
 *
 * <p>Adapter 从 VehicleMessageEnvelope 提取后向应用层传递；字段名与 presence 与
 * common generated class 对齐。不得创建 messageType/schemaVersion/causationId/payloadDigest
 * 别名作为第二事实。
 *
 * @param requestId        业务请求标识（跨一次业务操作的多个传输消息保持不变）
 * @param timestampMs      消息创建时间（Unix epoch ms）
 * @param protocolVersion  Envelope/车云消息协议版本
 * @param deviceId         设备标识
 * @param vin              车架号（敏感，仅协议承载）
 * @param vehicleTaskId    关联 VehicleTask（可空）
 * @param executionId      关联 Execution（可空）
 * @param idempotencyKey   写操作/可重试业务操作稳定幂等身份（可空）
 * @param payloadType      全限定业务消息类型
 * @param messageId        每条传输消息唯一标识
 * @param correlationId    关联 ID（可空）
 * @param messageKind      REQUEST/RESPONSE/EVENT
 * @param expireAtMs       绝对过期时间（可空）
 * @param traceContext     调用链关联（可空）
 * @author hwyz_leo
 */
public record FotaMessageMetadata(
        String requestId,
        long timestampMs,
        String protocolVersion,
        String deviceId,
        String vin,
        String vehicleTaskId,
        String executionId,
        String idempotencyKey,
        String payloadType,
        String messageId,
        String correlationId,
        MessageKind messageKind,
        Long expireAtMs,
        TraceContext traceContext) {

    public static FotaMessageMetadata fromEnvelope(vehicle.common.v1.Envelope.VehicleMessageEnvelope e) {
        return new FotaMessageMetadata(
                e.getRequestId(),
                e.getTimestampMs(),
                e.getProtocolVersion(),
                e.getDeviceId(),
                e.getVin(),
                e.hasVehicleTaskId() ? e.getVehicleTaskId() : null,
                e.hasExecutionId() ? e.getExecutionId() : null,
                e.hasIdempotencyKey() ? e.getIdempotencyKey() : null,
                e.getPayloadType(),
                e.getMessageId(),
                e.hasCorrelationId() ? e.getCorrelationId() : null,
                e.getMessageKind(),
                e.hasExpireAtMs() ? e.getExpireAtMs() : null,
                e.hasTraceContext() ? e.getTraceContext() : null);
    }
}
