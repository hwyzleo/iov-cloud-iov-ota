package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.OtaKafkaMessageHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.router.OtaMessageRouter;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageSchemaRegistry;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * OTA Kafka 入站处理流水线（CR-013 §5）
 *
 * <pre>
 * consume(kafkaRecord):
 *   validate envelope, schema, vin/device and payloadDigest
 *   begin transaction
 *   insert Inbox(consumerName, businessKey, digest, topic, partition, offset)
 *   if duplicate + same digest: 幂等，复用原结果
 *   if duplicate + different digest: 追加冲突结果到 Outbox
 *   invoke Application Service
 *   persist domain changes + Outbox(result/ack) in one transaction
 *   commit
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
public class OtaKafkaInboundHandler {

    private final OtaEnvelopeValidator envelopeValidator;
    private final OtaMessageSchemaRegistry schemaRegistry;
    private final OtaMessageRouter router;
    private final KafkaInboxRepository inboxRepository;
    private final KafkaMessagingMetricsService metrics;
    private final ObjectMapper objectMapper;

    /**
     * 处理一条 Kafka record（事务方法）。
     *
     * @throws OtaKafkaMessagingException 可恢复异常（回滚重投）或不可恢复异常（契约错误，由消费方转 DLQ）
     */
    @Transactional
    public void processMessage(ConsumerRecord<String, String> record) {
        OtaKafkaEnvelope envelope = parseEnvelope(record);
        envelopeValidator.validateStructure(envelope);
        envelopeValidator.validateTimestamp(envelope);
        if (!schemaRegistry.supports(envelope.getMessageType(), envelope.getSchemaVersion())) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "不支持的 messageType/schemaVersion: " + envelope.getMessageType() + "/" + envelope.getSchemaVersion());
        }

        OtaKafkaMessageHandler handler = router.resolve(envelope.getMessageType());
        JsonNode payload = envelope.getPayload();
        String consumerName = envelope.getMessageType();
        String businessKey = handler.businessKey(envelope, payload);
        String digest = resolveDigest(envelope, payload);

        // 同事务内锁定同业务键（唯一索引+间隙锁串行化同键并发）
        KafkaInboxPo existing = inboxRepository.selectForUpdate(consumerName, businessKey);
        if (existing != null) {
            metrics.increment(KafkaMessagingMetricsService.INBOX_DUPLICATE);
            if (existing.getPayloadDigest() != null && existing.getPayloadDigest().equals(digest)) {
                // 同键同摘要 → 幂等，复用原结果（结果消息已在 Outbox，由发布器投递）
                log.info("Inbox幂等命中：consumer[{}] businessKey[{}]，跳过重复处理", consumerName, businessKey);
                return;
            }
            // 同键异摘要 → 摘要冲突，生产业务拒绝事件
            metrics.increment(KafkaMessagingMetricsService.INBOX_DIGEST_CONFLICT);
            log.warn("Inbox摘要冲突：consumer[{}] businessKey[{}]，原摘要[{}] 新摘要[{}]",
                    consumerName, businessKey, existing.getPayloadDigest(), digest);
            Long conflictOutboxId = handler.handleConflict(envelope, payload,
                    "同业务唯一键不同 payload 摘要（幂等冲突）");
            inboxRepository.markResult(consumerName, businessKey, KafkaInboxPo.STATUS_CONFLICT,
                    conflictOutboxId, "摘要冲突");
            return;
        }

        // 首次处理：业务执行 + 领域状态 + 结果 Outbox 同事务
        Long resultOutboxId = handler.handle(envelope, payload);

        // 写入 Inbox 处理结果索引（同事务）
        KafkaInboxPo inboxPo = KafkaInboxPo.builder()
                .consumerName(consumerName)
                .businessKey(businessKey)
                .messageId(envelope.getMessageId() != null ? envelope.getMessageId() : record.key())
                .messageType(envelope.getMessageType())
                .schemaVersion(envelope.getSchemaVersion())
                .payloadDigest(digest)
                .kafkaTopic(record.topic())
                .kafkaPartition(record.partition())
                .kafkaOffset(record.offset())
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .resultMessageId(resultOutboxId)
                .build();
        inboxRepository.markProcessed(inboxPo);
        metrics.increment(KafkaMessagingMetricsService.INBOX_TOTAL);
        log.info("Kafka消息处理完成：type[{}] businessKey[{}] offset[{}]",
                consumerName, businessKey, record.offset());
    }

    private OtaKafkaEnvelope parseEnvelope(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("Kafka record 值为空");
        }
        try {
            return objectMapper.readValue(record.value(), OtaKafkaEnvelope.class);
        } catch (Exception e) {
            throw OtaKafkaMessagingException.nonRecoverable("Envelope JSON 解析失败: " + e.getMessage());
        }
    }

    private String resolveDigest(OtaKafkaEnvelope envelope, JsonNode payload) {
        if (envelope.getPayloadDigest() != null && !envelope.getPayloadDigest().isBlank()) {
            return envelope.getPayloadDigest();
        }
        try {
            String canonical = payload.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256:");
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw OtaKafkaMessagingException.nonRecoverable("payload 摘要计算失败: " + e.getMessage());
        }
    }
}
