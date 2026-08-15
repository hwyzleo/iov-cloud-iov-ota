package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.delivery;

import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vagw.v1.Delivery.GatewayDeliveryStatus;
import vagw.v1.Delivery.Outcome;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GatewayDeliveryStatus 校验器单元测试（CR-014 §7.2、验收 6）
 *
 * @author hwyz_leo
 */
@DisplayName("GatewayDeliveryStatusValidator - 独立技术投递状态校验")
class GatewayDeliveryStatusValidatorTest {

    private final GatewayDeliveryStatusValidator validator = new GatewayDeliveryStatusValidator();

    @Test
    @DisplayName("三种 Outcome（ACCEPTED/REJECTED/UNKNOWN）均合法通过")
    void all_outcomes_pass() {
        for (Outcome outcome : new Outcome[]{
                Outcome.OUTCOME_ACCEPTED, Outcome.OUTCOME_REJECTED, Outcome.OUTCOME_UNKNOWN}) {
            GatewayDeliveryStatus status = status(outcome);
            GatewayDeliveryStatus parsed = validator.validate(
                    new ConsumerRecord<>("iov.vagw.delivery.fota", 0, 0L, status.getVin(), status.toByteArray()));
            assertNotNull(parsed);
            assertEquals(outcome, parsed.getOutcome());
        }
    }

    @Test
    @DisplayName("Key ≠ status.vin → 不可恢复契约错误")
    void key_mismatch_fails() {
        GatewayDeliveryStatus status = status(Outcome.OUTCOME_ACCEPTED);
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validate(
                new ConsumerRecord<>("iov.vagw.delivery.fota", 0, 0L, "OTHER", status.toByteArray())));
    }

    @Test
    @DisplayName("缺少 original_message_id / vin / occurred_at_ms → 不可恢复契约错误")
    void missing_required_fails() {
        GatewayDeliveryStatus noMsgId = GatewayDeliveryStatus.newBuilder()
                .setVin("LSVAU2188N2ZG4G").setOutcome(Outcome.OUTCOME_ACCEPTED).setOccurredAtMs(1L).build();
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validate(
                new ConsumerRecord<>("iov.vagw.delivery.fota", 0, 0L, "LSVAU2188N2ZG4G", noMsgId.toByteArray())));

        GatewayDeliveryStatus noVin = GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1").setOutcome(Outcome.OUTCOME_ACCEPTED).setOccurredAtMs(1L).build();
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validate(
                new ConsumerRecord<>("iov.vagw.delivery.fota", 0, 0L, "LSVAU2188N2ZG4G", noVin.toByteArray())));
    }

    @Test
    @DisplayName("optional presence（correlation_id / retry_after_ms 缺失/存在）与未知字段兼容")
    void optional_presence_and_unknown_fields() {
        GatewayDeliveryStatus withUnknown = GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1")
                .setCorrelationId("corr-1")
                .setVin("LSVAU2188N2ZG4G")
                .setStage("MQTT_PUBLISHED")
                .setOutcome(Outcome.OUTCOME_REJECTED)
                .setReason("VEHICLE_OFFLINE")
                .setRetryable(true)
                .setRetryAfterMs(5000L)
                .setOccurredAtMs(System.currentTimeMillis())
                .build();
        GatewayDeliveryStatus parsed = validator.validate(new ConsumerRecord<>(
                "iov.vagw.delivery.fota", 0, 0L, "LSVAU2188N2ZG4G", withUnknown.toByteArray()));
        assertTrue(parsed.hasCorrelationId());
        assertTrue(parsed.hasRetryAfterMs());
        assertEquals(5000L, parsed.getRetryAfterMs());
    }

    private static GatewayDeliveryStatus status(Outcome outcome) {
        return GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1")
                .setVin("LSVAU2188N2ZG4G")
                .setStage("DOWNLINK_RECEIVED")
                .setOutcome(outcome)
                .setReason("ok")
                .setRetryable(false)
                .setOccurredAtMs(System.currentTimeMillis())
                .build();
    }
}
