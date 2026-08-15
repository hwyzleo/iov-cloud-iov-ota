package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import org.springframework.stereotype.Component;

/**
 * FOTA Outbox 追加器（CR-014 §6.1）
 *
 * <p>将冻结的 FotaOutboundEnvelope 在业务事务内追加到 tb_kafka_message_outbox；
 * 领域状态与 Outbox 同事务提交，重试只重发已持久化 bytes。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class FotaOutboxAppender {

    private final KafkaOutboxRepository outboxRepository;

    /**
     * @return 生成的 Outbox 消息 ID
     */
    public Long append(FotaOutboundEnvelope envelope) {
        return outboxRepository.append(KafkaOutboxPo.builder()
                .aggregateType(envelope.aggregateType())
                .aggregateId(envelope.aggregateId())
                .messageId(envelope.messageId())
                .payloadType(envelope.payloadType())
                .messageKind(envelope.messageKind())
                .correlationId(envelope.correlationId())
                .vin(envelope.vin())
                .envelopeBytes(envelope.envelopeBytes())
                .envelopeSha256(envelope.envelopeSha256())
                .build());
    }
}
