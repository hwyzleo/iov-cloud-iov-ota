package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.delivery;

import org.springframework.stereotype.Component;
import vagw.v1.Delivery.GatewayDeliveryStatus;

import java.time.Instant;

/**
 * 技术投递观测 Assembler（CR-014 §7.3）
 *
 * <p>将 GatewayDeliveryStatus 组装为脱敏观测摘要（VIN 仅 hash / 末位脱敏），
 * 不输出完整 VIN 或 payload。落库与重试语义由 DeliveryObservationService 统一处理。
 *
 * @author hwyz_leo
 */
@Component
public class DeliveryObservationAssembler {

    /**
     * 组装脱敏日志摘要（不含完整 VIN）。
     */
    public Summary summarize(GatewayDeliveryStatus status) {
        return new Summary(
                status.getOriginalMessageId(),
                status.hasCorrelationId() ? status.getCorrelationId() : null,
                maskVin(status.getVin()),
                status.getStage(),
                status.getOutcome() != null ? status.getOutcome().name() : null,
                status.getReason(),
                status.getRetryable(),
                status.hasRetryAfterMs() ? status.getRetryAfterMs() : null,
                latencyMs(status));
    }

    private static long latencyMs(GatewayDeliveryStatus status) {
        return Math.max(0L, Instant.now().toEpochMilli() - status.getOccurredAtMs());
    }

    private static String maskVin(String vin) {
        if (vin == null || vin.length() <= 4) {
            return "***";
        }
        return "***" + vin.substring(vin.length() - 4);
    }

    /**
     * 脱敏观测摘要。
     */
    public record Summary(String messageId, String correlationId, String vinMasked, String stage,
                          String outcome, String reason, boolean retryable, Long retryAfterMs,
                          long latencyMs) {
    }
}
