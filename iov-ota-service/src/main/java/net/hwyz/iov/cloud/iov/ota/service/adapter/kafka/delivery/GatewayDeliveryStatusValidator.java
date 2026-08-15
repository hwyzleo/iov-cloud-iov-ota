package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.delivery;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import vagw.v1.Delivery.GatewayDeliveryStatus;
import vagw.v1.Delivery.Outcome;

/**
 * GatewayDeliveryStatus 强类型校验器（CR-014 §7.2）
 *
 * <p>Key 必须等于 status.vin；original_message_id/vin/occurred_at 必填；
 * Outcome 未知枚举/未知字段由 Protobuf 兼容机制保留，不做 JSON fallback。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class GatewayDeliveryStatusValidator {

    /**
     * 校验一条技术投递状态 record。
     *
     * @throws OtaKafkaMessagingException 不可恢复契约错误
     */
    public GatewayDeliveryStatus validate(ConsumerRecord<String, byte[]> record) {
        byte[] raw = record.value();
        if (raw == null || raw.length == 0) {
            throw OtaKafkaMessagingException.nonRecoverable("GatewayDeliveryStatus record 值为空");
        }
        GatewayDeliveryStatus status;
        try {
            status = GatewayDeliveryStatus.parseFrom(raw);
        } catch (Exception e) {
            throw OtaKafkaMessagingException.nonRecoverable("GatewayDeliveryStatus 解析失败: " + e.getMessage());
        }
        if (record.key() == null || !record.key().equals(status.getVin())) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "Kafka Key 与 GatewayDeliveryStatus.vin 不一致: key["
                            + FotaEnvelopeValidator.maskVin(record.key()) + "] vin["
                            + FotaEnvelopeValidator.maskVin(status.getVin()) + "]");
        }
        if (status.getOriginalMessageId() == null || status.getOriginalMessageId().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 original_message_id");
        }
        if (status.getVin() == null || status.getVin().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 vin");
        }
        if (status.getOccurredAtMs() <= 0) {
            throw OtaKafkaMessagingException.nonRecoverable("occurred_at_ms 非法");
        }
        if (status.getOutcome() == null) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 outcome");
        }
        if (status.getOutcome() != Outcome.OUTCOME_UNSPECIFIED) {
            log.debug("技术投递状态已解析：messageId[{}] stage[{}] outcome[{}]",
                    status.getOriginalMessageId(), status.getStage(), status.getOutcome());
        }
        return status;
    }
}
