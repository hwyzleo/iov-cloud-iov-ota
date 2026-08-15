package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FOTA 下行 Envelope 生产者（CR-014 §6.2）
 *
 * <p>轮询 tb_kafka_message_outbox 待发布（PENDING）消息：
 * <ol>
 *   <li>原子认领（PENDING→PUBLISHING）避免重复生产；</li>
 *   <li>原样发送已持久化的完整 Envelope bytes 到下行 topic（Key=Envelope.vin）；</li>
 *   <li>成功标记 PUBLISHED；失败按指数退避重试，超限转 DEAD 死信（人工可观测，不丢弃领域事实）。</li>
 * </ol>
 *
 * <p>重试只重发已持久化 bytes，不重建 Envelope、不生成新 message_id、不改变 correlation/trace。
 * payload=10 是唯一业务 payload；不增加外层 JSON、digest、compression、seq、ttl_ms 或 schemaVersion。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FotaEnvelopeProducer {

    private final ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    private final KafkaOutboxRepository outboxRepository;
    private final OtaKafkaProperties properties;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 定时轮询并发布待处理的下行 Envelope。
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
        byte[] value = po.getEnvelopeBytes();
        if (value == null || value.length == 0) {
            outboxRepository.markDead(po.getId(), "Envelope bytes 缺失");
            metrics.increment(KafkaMessagingMetricsService.OUTBOX_DEAD);
            return;
        }
        String topic = properties.getOutbound().getTopic();
        String key = po.getVin() != null ? po.getVin() : po.getAggregateId();
        producerTemplate.send(topic, key, value)
                .subscribe(
                        result -> {
                            outboxRepository.markPublished(po.getId());
                            metrics.increment(KafkaMessagingMetricsService.OUTBOX_PUBLISHED);
                            log.debug("FOTA 下行 Envelope 已发布：payloadType[{}] key[{}] offset[{}]",
                                    po.getPayloadType(), FotaEnvelopeValidator.maskVin(key),
                                    result.recordMetadata().offset());
                        },
                        error -> onPublishError(po, error));
    }

    private void onPublishError(KafkaOutboxPo po, Throwable error) {
        int retry = (po.getRetryCount() == null ? 0 : po.getRetryCount()) + 1;
        int maxRetry = properties.getOutbound().getMaxRetry();
        if (retry >= maxRetry) {
            outboxRepository.markDead(po.getId(), error.getMessage());
            metrics.increment(KafkaMessagingMetricsService.OUTBOX_DEAD);
            log.error("FOTA 下行 Envelope 超过最大重试次数转死信：id[{}] type[{}] error[{}]",
                    po.getId(), po.getPayloadType(), error.getMessage(), error);
            return;
        }
        long backoff = properties.getOutbound().getBackoffBaseSeconds()
                * (long) Math.pow(2, Math.max(0, retry - 1));
        outboxRepository.markFailed(po.getId(), error.getMessage(), backoff);
        metrics.increment(KafkaMessagingMetricsService.OUTBOX_RETRY);
        log.warn("FOTA 下行 Envelope 发布失败，{}/{} 次后重试（{}s）：id[{}] type[{}] error[{}]",
                retry, maxRetry, backoff, po.getId(), po.getPayloadType(), error.getMessage());
    }
}
