package net.hwyz.iov.cloud.iov.ota.service.application.messaging.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.delivery.DeliveryObservationRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vagw.v1.Delivery.GatewayDeliveryStatus;
import vagw.v1.Delivery.Outcome;

/**
 * VAGW 技术投递观测服务（CR-014 §7.2/§9）
 *
 * <p>语义：
 * <ul>
 *   <li>OUTCOME_ACCEPTED：仅记录 VAGW/MQTT 技术接管，等待正式 FOTA RESPONSE/EVENT；</li>
 *   <li>OUTCOME_REJECTED：记录拒绝，仅在 retryable=true、retry_after_ms、命令有效期、
 *       幂等键与领域策略均允许时受控重试（以 Outbox retry_count 上限约束，不得无限重发）；</li>
 *   <li>OUTCOME_UNKNOWN：标记结果未知，不得提升为成功，不自动重发不可幂等命令，进入对账/观测。</li>
 * </ul>
 * 技术状态不得直接将 VehicleTask/Execution 置为 SUCCEEDED/FAILED 或业务已受理。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryObservationService {

    private final DeliveryObservationRepository observationRepository;
    private final KafkaOutboxRepository outboxRepository;
    private final KafkaMessagingMetricsService metrics;

    /**
     * 记录一次技术投递状态（幂等）。
     */
    @Transactional
    public void record(GatewayDeliveryStatus status) {
        Outcome outcome = status.getOutcome();
        if (outcome == null || outcome == Outcome.OUTCOME_UNSPECIFIED) {
            metrics.increment("delivery.unspecified");
            log.warn("技术投递状态 Outcome 未指定：messageId[{}]", status.getOriginalMessageId());
            return;
        }
        if (observationRepository.duplicate(status.getOriginalMessageId(), status.getStage(), status.getOccurredAtMs())) {
            metrics.increment("delivery.duplicate");
            log.debug("技术投递观测幂等命中：messageId[{}] stage[{}]", status.getOriginalMessageId(), status.getStage());
            return;
        }
        observationRepository.insert(status);

        switch (outcome) {
            case OUTCOME_ACCEPTED -> {
                metrics.increment("delivery.accepted");
                log.info("技术投递已接管（等待正式 FOTA 响应）：messageId[{}] stage[{}]",
                        status.getOriginalMessageId(), status.getStage());
            }
            case OUTCOME_REJECTED -> {
                metrics.increment("delivery.rejected");
                if (status.getRetryable() && status.hasRetryAfterMs()) {
                    scheduleControlledRetry(status);
                } else {
                    log.warn("技术投递被拒绝（不重试）：messageId[{}] stage[{}] reason[{}]",
                            status.getOriginalMessageId(), status.getStage(), status.getReason());
                }
            }
            case OUTCOME_UNKNOWN -> {
                metrics.increment("delivery.unknown");
                log.warn("技术投递结果未知（对账/观测，不提升成功）：messageId[{}] stage[{}] reason[{}]",
                        status.getOriginalMessageId(), status.getStage(), status.getReason());
            }
            default -> {
                // 不可达
            }
        }
    }

    /**
     * 受控重试：将原下行 Outbox 消息重新入队（PENDING），以 retry_count 上限约束，不得无限重发。
     */
    private void scheduleControlledRetry(GatewayDeliveryStatus status) {
        KafkaOutboxPo original = outboxRepository.findByMessageId(status.getOriginalMessageId());
        if (original == null) {
            metrics.increment("delivery.retry.skip.no_outbox");
            log.info("投递重试跳过（原消息已不在 Outbox）：messageId[{}]", status.getOriginalMessageId());
            return;
        }
        int retry = original.getRetryCount() == null ? 0 : original.getRetryCount();
        if (retry >= 5) {
            metrics.increment("delivery.retry.skip.limit");
            log.warn("投递重试跳过（超过重试上限）：messageId[{}] retry[{}]", status.getOriginalMessageId(), retry);
            return;
        }
        outboxRepository.requeue(original.getId(), status.getRetryAfterMs());
        metrics.increment("delivery.retry.scheduled");
        log.info("技术投递受控重试已安排：messageId[{}] retry_after_ms[{}]", status.getOriginalMessageId(), status.getRetryAfterMs());
    }
}
