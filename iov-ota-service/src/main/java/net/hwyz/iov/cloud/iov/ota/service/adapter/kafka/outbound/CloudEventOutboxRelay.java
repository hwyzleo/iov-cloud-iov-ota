package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OTA→VMD 云服务事件 Relay（CR-019 §7）
 *
 * <p>轮询 tb_cloud_event_outbox 待发布（PENDING）云事件：
 * <ol>
 *   <li>原子认领（PENDING→PUBLISHING）避免重复生产；</li>
 *   <li>发送 payload JSON 到目标 Topic（Key=VIN）；</li>
 *   <li>成功标记 PUBLISHED；失败指数退避重试，超限转 DEAD。</li>
 * </ol>
 *
 * <p>门禁（对齐 MDM-DSN-CR-034）：KafkaTopicProvisioningStatus = NOT_READY 时暂停本轮
 * （Topic 尚未检查/创建完成，不发送、不累加重试）；READY / DISABLED 放行。
 *
 * <p>云服务消息不使用 Proto、不归 PAR-PROTO 管理、不进入 vehicle.fota.v1。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudEventOutboxRelay {

    private final ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    private final CloudEventOutboxRepository outboxRepository;
    private final OtaKafkaProperties properties;
    private final ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatusProvider;

    @Scheduled(fixedDelayString = "${ota.kafka.cloud-events.poll-interval-ms:2000}")
    public void publishPending() {
        if (!properties.getCloudEvents().isEnabled()) {
            return;
        }
        // 门禁：NOT_READY 时暂停本轮，READY / DISABLED（或未装配）放行
        KafkaTopicProvisioningStatus provisioningStatus = provisioningStatusProvider.getIfAvailable();
        KafkaTopicProvisioningStatus.State state = provisioningStatus == null
                ? KafkaTopicProvisioningStatus.State.DISABLED : provisioningStatus.state();
        if (state == KafkaTopicProvisioningStatus.State.NOT_READY) {
            log.info("Kafka Topic 未全部就绪，暂停云服务事件 Relay: provisioningState={}", state);
            return;
        }
        if (state == KafkaTopicProvisioningStatus.State.DISABLED) {
            log.debug("Kafka Topic Provisioning 已显式停用，走兼容路径: provisioningState=DISABLED");
        }
        List<CloudEventOutboxPo> pending = outboxRepository.findPendingReady(
                properties.getCloudEvents().getBatchSize());
        if (pending.isEmpty()) {
            return;
        }
        for (CloudEventOutboxPo po : pending) {
            if (outboxRepository.claim(po.getId())) {
                publish(po);
            }
        }
    }

    private void publish(CloudEventOutboxPo po) {
        if (po.getPayloadJson() == null || po.getPayloadJson().isBlank()) {
            outboxRepository.markDead(po.getId(), "payload JSON 缺失");
            return;
        }
        byte[] value = po.getPayloadJson().getBytes(StandardCharsets.UTF_8);
        String topic = po.getTopic() != null ? po.getTopic()
                : properties.getCloudEvents().getObservedTopic();
        String key = po.getVin() != null ? po.getVin() : po.getBusinessKey();
        producerTemplate.send(topic, key, value)
                .subscribe(
                        result -> {
                            outboxRepository.markPublished(po.getId());
                            log.debug("云服务事件已发布：type[{}] key[{}] offset[{}]",
                                    po.getEventType(), mask(key), result.recordMetadata().offset());
                        },
                        error -> onPublishError(po, error));
    }

    private void onPublishError(CloudEventOutboxPo po, Throwable error) {
        int retry = (po.getRetryCount() == null ? 0 : po.getRetryCount()) + 1;
        int maxRetry = properties.getCloudEvents().getMaxRetry();
        if (retry >= maxRetry) {
            outboxRepository.markDead(po.getId(), error.getMessage());
            log.error("云服务事件超过最大重试次数转死信：id[{}] type[{}] error[{}]",
                    po.getId(), po.getEventType(), error.getMessage(), error);
            return;
        }
        long backoff = properties.getCloudEvents().getBackoffBaseSeconds()
                * (long) Math.pow(2, Math.max(0, retry - 1));
        outboxRepository.markFailed(po.getId(), error.getMessage(), backoff);
        log.warn("云服务事件发布失败，{}/{} 次后重试（{}s）：id[{}] type[{}] error[{}]",
                retry, maxRetry, backoff, po.getId(), po.getEventType(), error.getMessage());
    }

    private static String mask(String key) {
        if (key == null || key.length() <= 4) {
            return "***";
        }
        return "***" + key.substring(key.length() - 4);
    }
}
