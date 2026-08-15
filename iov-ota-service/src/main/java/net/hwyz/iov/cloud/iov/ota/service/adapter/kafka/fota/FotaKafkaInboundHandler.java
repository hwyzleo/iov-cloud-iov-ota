package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;

/**
 * FOTA 入站处理流水线（CR-014 §4.2）
 *
 * <pre>
 * consume(record):
 *   raw = record.value
 *   envelope = VehicleMessageEnvelope.parseFrom(raw)
 *   validate record.key == envelope.vin
 *   validate service == vehicle.fota / protocol major / required / TTL / size
 *   validate payload_type in registry / message_kind direction matrix
 *   handler = payloadRouter.resolve(payload_type)
 *   typedPayload = handler.parse(envelope.payload)
 *   inboxResult = inbox.claim(message_id, sha256(raw))
 *   duplicate same hash -> 幂等复用；duplicate different hash -> 冲突隔离
 *   invoke handler.handle(metadata, typedPayload)
 * </pre>
 *
 * <p>可恢复异常抛出（事务回滚、offset 不提交、由 Kafka 重投）；
 * 不可恢复契约错误由消费方转 DLQ/隔离并生产业务拒绝事件。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FotaKafkaInboundHandler {

    private final FotaEnvelopeValidator validator;
    private final FotaPayloadRouter router;
    private final KafkaInboxRepository inboxRepository;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 处理一条 Kafka record（事务方法）。
     *
     * @throws OtaKafkaMessagingException 可恢复异常（回滚重投）或不可恢复异常（契约错误，由消费方转 DLQ）
     */
    @Transactional
    public void processMessage(ConsumerRecord<String, byte[]> record) {
        byte[] raw = record.value();
        if (raw == null || raw.length == 0) {
            throw OtaKafkaMessagingException.nonRecoverable("Kafka record 值为空");
        }
        VehicleMessageEnvelope envelope;
        try {
            envelope = VehicleMessageEnvelope.parseFrom(raw);
        } catch (InvalidProtocolBufferException e) {
            throw OtaKafkaMessagingException.nonRecoverable("Envelope 解析失败: " + e.getMessage());
        }
        validator.validate(record, envelope);

        String payloadType = envelope.getPayloadType();
        @SuppressWarnings("rawtypes")
        FotaPayloadHandler handler = router.resolve(payloadType);
        FotaMessageMetadata metadata = FotaMessageMetadata.fromEnvelope(envelope);

        com.google.protobuf.Message typedPayload = parsePayload(handler, envelope);
        String consumerName = payloadType;
        String sha = FotaDigests.sha256(raw);

        // 同事务内锁定同 message_id（唯一索引+间隙锁串行化同键并发）
        KafkaInboxPo existing = inboxRepository.selectForUpdate(consumerName, envelope.getMessageId());
        if (existing != null) {
            metrics.increment(KafkaMessagingMetricsService.INBOX_DUPLICATE);
            if (existing.getEnvelopeSha256() != null && existing.getEnvelopeSha256().equals(sha)) {
                // 同 message_id 同摘要 → 幂等，复用原结果
                log.info("Inbox 幂等命中：payloadType[{}] messageId[{}]，跳过重复处理", payloadType, envelope.getMessageId());
                return;
            }
            // 同 message_id 异摘要 → 冲突隔离，生产业务拒绝事件
            metrics.increment(KafkaMessagingMetricsService.INBOX_DIGEST_CONFLICT);
            log.warn("Inbox Envelope 摘要冲突：payloadType[{}] messageId[{}] 原摘要[{}] 新摘要[{}]",
                    payloadType, envelope.getMessageId(), existing.getEnvelopeSha256(), sha);
            handler.handleConflict(metadata, typedPayload, "同 message_id 不同 Envelope 摘要（冲突隔离）");
            inboxRepository.markConflict(consumerName, envelope.getMessageId(), "Envelope 摘要冲突");
            return;
        }

        // 首次处理：业务执行 + 领域状态 + 结果 Outbox 同事务
        Long resultOutboxId = handler.handle(metadata, typedPayload);

        KafkaInboxPo inboxPo = KafkaInboxPo.builder()
                .consumerName(consumerName)
                .messageId(envelope.getMessageId())
                .envelopeSha256(sha)
                .payloadType(payloadType)
                .messageKind(envelope.getMessageKind().name())
                .protocolVersion(envelope.getProtocolVersion())
                .vin(envelope.getVin())
                .kafkaTopic(record.topic())
                .kafkaPartition(record.partition())
                .kafkaOffset(record.offset())
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .resultMessageId(resultOutboxId)
                .build();
        inboxRepository.markProcessed(inboxPo);
        metrics.increment(KafkaMessagingMetricsService.INBOX_TOTAL);
        log.info("FOTA 消息处理完成：payloadType[{}] messageId[{}] offset[{}]",
                payloadType, envelope.getMessageId(), record.offset());
    }

    @SuppressWarnings("rawtypes")
    private com.google.protobuf.Message parsePayload(FotaPayloadHandler handler, VehicleMessageEnvelope envelope) {
        try {
            return handler.parse(envelope.getPayload());
        } catch (InvalidProtocolBufferException e) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "FOTA payload 解析失败: " + envelope.getPayloadType() + " " + e.getMessage());
        }
    }
}
