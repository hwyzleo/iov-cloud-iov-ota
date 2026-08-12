package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageSchemaRegistry;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * OTA Kafka 下行消息生产者（CR-013 §5/§7）
 *
 * <p>轮询 tb_kafka_message_outbox 待发布（PENDING）消息：
 * <ol>
 *   <li>原子认领（PENDING→PUBLISHING）避免重复生产；</li>
 *   <li>组装统一 Envelope 生产到下行 topic（message_key 为 vin/executionId）；</li>
 *   <li>成功标记 PUBLISHED；失败按指数退避重试，超限转 DEAD 死信（人工可观测，不丢弃领域事实）。</li>
 * </ol>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtaKafkaMessageProducer {

    private final ReactiveKafkaProducerTemplate<String, String> producerTemplate;
    private final KafkaOutboxRepository outboxRepository;
    private final OtaKafkaProperties properties;
    private final KafkaMessagingMetricsService metrics;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT;

    /**
     * 定时轮询并发布待处理的下行消息。
     */
    @Scheduled(fixedDelayString = "${ota.kafka.outbound.poll-interval-ms:2000}")
    public void publishPending() {
        if (!properties.getOutbound().isEnabled()) {
            return;
        }
        List<KafkaOutboxPo> pending = outboxRepository.findPendingReady(properties.getOutbound().getBatchSize());
        if (pending.isEmpty()) {
            return;
        }
        for (KafkaOutboxPo po : pending) {
            // 原子认领，只有成功的才发布
            if (outboxRepository.claim(po.getId())) {
                publish(po);
            }
        }
    }

    private void publish(KafkaOutboxPo po) {
        String value = buildEnvelopeJson(po);
        if (value == null) {
            outboxRepository.markDead(po.getId(), "Envelope 组装失败");
            metrics.increment(KafkaMessagingMetricsService.OUTBOX_DEAD);
            return;
        }
        String topic = properties.getOutbound().getTopic();
        String key = po.getMessageKey() != null ? po.getMessageKey() : po.getVin();
        producerTemplate.send(topic, key, value)
                .subscribe(
                        result -> {
                            outboxRepository.markPublished(po.getId());
                            metrics.increment(KafkaMessagingMetricsService.OUTBOX_PUBLISHED);
                            log.debug("Kafka下行消息已发布：type[{}] key[{}] offset[{}]",
                                    po.getMessageType(), key, result.recordMetadata().offset());
                        },
                        error -> onPublishError(po, error));
    }

    private void onPublishError(KafkaOutboxPo po, Throwable error) {
        int retry = (po.getRetryCount() == null ? 0 : po.getRetryCount()) + 1;
        int maxRetry = properties.getOutbound().getMaxRetry();
        if (retry >= maxRetry) {
            outboxRepository.markDead(po.getId(), error.getMessage());
            metrics.increment(KafkaMessagingMetricsService.OUTBOX_DEAD);
            log.error("Kafka下行消息超过最大重试次数转死信：id[{}] type[{}] error[{}]",
                    po.getId(), po.getMessageType(), error.getMessage(), error);
            return;
        }
        long backoff = properties.getOutbound().getBackoffBaseSeconds()
                * (long) Math.pow(2, Math.max(0, retry - 1));
        outboxRepository.markFailed(po.getId(), error.getMessage(), backoff);
        metrics.increment(KafkaMessagingMetricsService.OUTBOX_RETRY);
        log.warn("Kafka下行消息发布失败，{}/{} 次后重试（{}s）：id[{}] type[{}] error[{}]",
                retry, maxRetry, backoff, po.getId(), po.getMessageType(), error.getMessage());
    }

    /**
     * 组装统一 Kafka Envelope JSON。
     */
    private String buildEnvelopeJson(KafkaOutboxPo po) {
        try {
            String payloadJson = po.getPayloadJson();
            OtaKafkaEnvelope envelope = OtaKafkaEnvelope.builder()
                    .messageId(UUID.randomUUID().toString())
                    .messageType(po.getMessageType())
                    .schemaVersion(OtaMessageSchemaRegistry.SUPPORTED_SCHEMA_VERSION)
                    .timestamp(ISO_FORMAT.format(Instant.now()))
                    .vin(po.getVin())
                    .correlationId(po.getCorrelationId())
                    .payloadDigest(computeDigest(payloadJson))
                    .payload(objectMapper.readTree(payloadJson))
                    .build();
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            log.error("组装Kafka Envelope失败：id[{}]", po.getId(), e);
            return null;
        }
    }

    private String computeDigest(String payloadJson) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payloadJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256:");
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("摘要计算失败", e);
        }
    }
}
