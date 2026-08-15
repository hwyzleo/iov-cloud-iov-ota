package net.hwyz.iov.cloud.iov.ota.service.application.messaging.delivery;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.delivery.DeliveryObservationRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vagw.v1.Delivery.GatewayDeliveryStatus;
import vagw.v1.Delivery.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * DeliveryObservationService 单元测试（CR-014 §7.2/§9、验收 6）
 *
 * <p>三种 Outcome 语义与受控重试（REJECTED + retryable → requeue，上限约束；UNKNOWN 不提升成功）。
 *
 * @author hwyz_leo
 */
@DisplayName("DeliveryObservationService - 三种 Outcome 语义与受控重试")
class DeliveryObservationServiceTest {

    private DeliveryObservationRepository observationRepository;
    private KafkaOutboxRepository outboxRepository;
    private DeliveryObservationService service;

    @BeforeEach
    void setUp() {
        observationRepository = mock(DeliveryObservationRepository.class);
        outboxRepository = mock(KafkaOutboxRepository.class);
        service = new DeliveryObservationService(observationRepository, outboxRepository,
                mock(KafkaMessagingMetricsService.class));
    }

    @Test
    @DisplayName("OUTCOME_ACCEPTED：仅记录技术接管，不重试、不推进")
    void accepted_records_only() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(false);
        service.record(status(Outcome.OUTCOME_ACCEPTED, false, false));
        verify(observationRepository, times(1)).insert(any());
        verify(outboxRepository, never()).requeue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("OUTCOME_REJECTED + retryable + 有 retry_after：受控重试（requeue 原消息）")
    void rejected_retryable_requeues() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(false);
        KafkaOutboxPo original = KafkaOutboxPo.builder().id(7L).retryCount(0).build();
        when(outboxRepository.findByMessageId("m1")).thenReturn(original);

        GatewayDeliveryStatus status = GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1")
                .setVin("LSVAU2188N2ZG4G")
                .setStage("MQTT_PUBLISH_FAILED")
                .setOutcome(Outcome.OUTCOME_REJECTED)
                .setReason("VEHICLE_OFFLINE")
                .setRetryable(true)
                .setRetryAfterMs(3000L)
                .setOccurredAtMs(System.currentTimeMillis())
                .build();
        service.record(status);

        verify(outboxRepository).requeue(7L, 3000L);
    }

    @Test
    @DisplayName("OUTCOME_REJECTED 不可重试：记录拒绝但不重发")
    void rejected_non_retryable_no_requeue() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(false);
        service.record(status(Outcome.OUTCOME_REJECTED, false, false));
        verify(outboxRepository, never()).requeue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("OUTCOME_REJECTED + retryable 但原消息不在 Outbox：不重发")
    void rejected_retryable_no_outbox() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(false);
        when(outboxRepository.findByMessageId("m1")).thenReturn(null);
        GatewayDeliveryStatus status = GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1").setVin("LSVAU2188N2ZG4G").setStage("s")
                .setOutcome(Outcome.OUTCOME_REJECTED).setReason("r").setRetryable(true)
                .setRetryAfterMs(1000L).setOccurredAtMs(System.currentTimeMillis()).build();
        service.record(status);
        verify(outboxRepository, never()).requeue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("OUTCOME_UNKNOWN：标记结果未知，不自动重发、不提升成功")
    void unknown_no_requeue() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(false);
        service.record(status(Outcome.OUTCOME_UNKNOWN, true, true));
        verify(observationRepository, times(1)).insert(any());
        verify(outboxRepository, never()).requeue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("同 (message_id, stage, occurred_at_ms) 幂等：不重复插入")
    void duplicate_is_ignored() {
        when(observationRepository.duplicate(anyString(), anyString(), anyLong())).thenReturn(true);
        service.record(status(Outcome.OUTCOME_ACCEPTED, false, false));
        verify(observationRepository, never()).insert(any());
    }

    private static GatewayDeliveryStatus status(Outcome outcome, boolean retryable, boolean withRetryAfter) {
        GatewayDeliveryStatus.Builder b = GatewayDeliveryStatus.newBuilder()
                .setOriginalMessageId("m1")
                .setVin("LSVAU2188N2ZG4G")
                .setStage("DOWNLINK_RECEIVED")
                .setOutcome(outcome)
                .setReason("reason")
                .setRetryable(retryable)
                .setOccurredAtMs(System.currentTimeMillis());
        if (withRetryAfter) {
            b.setRetryAfterMs(2000L);
        }
        return b.build();
    }
}
