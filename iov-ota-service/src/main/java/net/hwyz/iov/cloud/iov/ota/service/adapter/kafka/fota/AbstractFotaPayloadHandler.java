package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.Message;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;

/**
 * FOTA payload 处理器抽象基类（CR-014 §5）
 *
 * <p>子类定义 payload_type/message_kind/businessKey/业务执行；响应 Envelope 由
 * {@link FotaEnvelopeFactory} 冻结字节后经 {@link FotaOutboxAppender} 与领域状态同事务入 Outbox。
 *
 * @param <T> generated 强类型
 * @author hwyz_leo
 */
public abstract class AbstractFotaPayloadHandler<T extends Message> implements FotaPayloadHandler<T> {

    protected final FotaEnvelopeFactory envelopeFactory;
    protected final FotaOutboxAppender outboxAppender;
    protected final KafkaMessagingMetricsService metrics;

    protected AbstractFotaPayloadHandler(FotaEnvelopeFactory envelopeFactory,
                                         FotaOutboxAppender outboxAppender,
                                         KafkaMessagingMetricsService metrics) {
        this.envelopeFactory = envelopeFactory;
        this.outboxAppender = outboxAppender;
        this.metrics = metrics;
    }

    /**
     * 将响应 proto 冻结为 RESPONSE Envelope 并追加 Outbox（与领域状态同事务）。
     *
     * @return Outbox 消息 ID
     */
    protected Long appendResponse(FotaMessageMetadata md, Message response,
                                  String aggregateType, String aggregateId) {
        FotaOutboundEnvelope envelope = envelopeFactory.response(md, response, aggregateType, aggregateId);
        return outboxAppender.append(envelope);
    }
}
