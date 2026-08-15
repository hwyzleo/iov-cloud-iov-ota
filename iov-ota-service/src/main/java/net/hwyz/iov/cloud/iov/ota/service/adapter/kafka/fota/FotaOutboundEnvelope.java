package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

/**
 * FOTA 出站 Envelope（CR-014 §6.2/§6.3）
 *
 * <p>由 {@link FotaEnvelopeFactory} 冻结生成；envelopeBytes 在 Outbox 首次创建时落库，
 * 重试只重发已持久化 bytes，不重建 Envelope、不改 message_id/correlation/trace。
 *
 * @param messageId    传输消息唯一 ID
 * @param payloadType  全限定 payload_type
 * @param messageKind  REQUEST/RESPONSE/EVENT
 * @param correlationId 关联 ID（RESPONSE 指向请求 message_id）
 * @param vin          车架号（Kafka Key=Envelope.vin）
 * @param envelopeBytes 冻结的完整序列化 Envelope bytes
 * @param envelopeSha256 Envelope bytes SHA-256
 * @param aggregateType 聚合类型（TASK/VEHICLE_TASK/EXECUTION）
 * @param aggregateId  聚合 ID
 * @author hwyz_leo
 */
public record FotaOutboundEnvelope(
        String messageId,
        String payloadType,
        String messageKind,
        String correlationId,
        String vin,
        byte[] envelopeBytes,
        String envelopeSha256,
        String aggregateType,
        String aggregateId) {
}
