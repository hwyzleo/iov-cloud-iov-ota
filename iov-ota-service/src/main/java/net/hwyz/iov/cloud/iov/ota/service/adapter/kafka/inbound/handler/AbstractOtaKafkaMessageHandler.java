package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;

/**
 * OTA Kafka 消息处理器抽象基类（CR-013 §4/§5）
 *
 * <p>提供 payload 解析、结果/拒绝消息入 Outbox 的公共能力。
 * 子类定义：消息类型、业务唯一键、业务执行与冲突结果。
 *
 * @author hwyz_leo
 */
public abstract class AbstractOtaKafkaMessageHandler implements OtaKafkaMessageHandler {

    protected final ObjectMapper objectMapper;
    protected final KafkaOutboxRepository outboxRepository;
    protected final KafkaMessagingMetricsService metrics;

    protected AbstractOtaKafkaMessageHandler(ObjectMapper objectMapper,
                                             KafkaOutboxRepository outboxRepository,
                                             KafkaMessagingMetricsService metrics) {
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
        this.metrics = metrics;
    }

    /**
     * 将 payload JSON 节点转换为命令 DTO。
     */
    protected <T> T treeToValue(JsonNode payload, Class<T> clazz) {
        return objectMapper.convertValue(payload, clazz);
    }

    /**
     * 追加一条下行结果消息到 Outbox（与领域状态同事务）。
     */
    protected Long appendResult(String aggregateType, String aggregateId, OtaMessageType messageType,
                                String messageKey, OtaKafkaEnvelope envelope, Object payload) {
        return outboxRepository.append(KafkaOutboxPo.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .messageType(messageType.getValue())
                .messageKey(messageKey)
                .correlationId(envelope.getCorrelationId())
                .vin(envelope.getVin())
                .payloadJson(toJson(payload))
                .build());
    }

    /**
     * 追加一条拒绝/冲突结果消息到 Outbox。
     */
    protected Long appendRejected(String aggregateType, String aggregateId, OtaMessageType messageType,
                                  String messageKey, OtaKafkaEnvelope envelope, String code, String reason) {
        return outboxRepository.append(KafkaOutboxPo.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .messageType(messageType.getValue())
                .messageKey(messageKey)
                .correlationId(envelope.getCorrelationId())
                .vin(envelope.getVin())
                .payloadJson(toJson(OtaKafkaReject.of(code, reason)))
                .build());
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("序列化下行消息失败", e);
        }
    }
}
