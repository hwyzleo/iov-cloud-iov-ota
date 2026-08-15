package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.GatewayDeliveryObservationMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.GatewayDeliveryObservationPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vagw.v1.Delivery.GatewayDeliveryStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * VAGW 技术投递观测仓库（CR-014 §8）
 *
 * <p>以 UK(original_message_id, stage, occurred_at_ms) 幂等写入；
 * VIN 仅存 SHA-256，不落原文。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DeliveryObservationRepository {

    private final GatewayDeliveryObservationMapper mapper;

    /**
     * 是否已存在同 (original_message_id, stage, occurred_at_ms) 观测。
     */
    @Transactional
    public boolean duplicate(String originalMessageId, String stage, long occurredAtMs) {
        return mapper.selectUnique(originalMessageId, stage, occurredAtMs) != null;
    }

    /**
     * 插入一条投递观测。
     */
    @Transactional
    public void insert(GatewayDeliveryStatus status) {
        GatewayDeliveryObservationPo po = GatewayDeliveryObservationPo.builder()
                .originalMessageId(status.getOriginalMessageId())
                .correlationId(status.hasCorrelationId() ? status.getCorrelationId() : null)
                .vinHash(sha256(status.getVin()))
                .stage(status.getStage())
                .outcome(status.getOutcome().name())
                .reason(status.getReason())
                .retryable(status.getRetryable())
                .retryAfterMs(status.hasRetryAfterMs() ? status.getRetryAfterMs() : null)
                .occurredAtMs(status.getOccurredAtMs())
                .build();
        mapper.insert(po);
    }

    private static String sha256(String vin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(vin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
