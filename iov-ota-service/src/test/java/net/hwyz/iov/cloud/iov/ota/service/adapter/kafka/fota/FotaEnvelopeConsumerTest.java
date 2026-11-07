package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound.OtaKafkaDlqService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FOTA 上行消费者 DLQ/offset 契约测试（CR-014 §4.1 / CR-020 §4.3）
 *
 * <p>契约错误的 offset 必须「DLQ 转存成功后才提交」：转存失败时不得 ack，
 * 否则消息既未进入 DLQ 也已被消费，会永久丢失、无法回放。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FotaEnvelopeConsumer DLQ 与 offset 提交顺序")
class FotaEnvelopeConsumerTest {

    @Mock private FotaKafkaInboundHandler inboundHandler;
    @Mock private OtaKafkaDlqService dlqService;
    @Mock private Acknowledgment acknowledgment;

    private FotaEnvelopeConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FotaEnvelopeConsumer(inboundHandler, dlqService, new KafkaMessagingMetricsService());
    }

    private static ConsumerRecord<String, byte[]> record() {
        return new ConsumerRecord<>("vagw.fota", 0, 0L, "LSVAU2188N2ZG4G", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("业务处理成功提交 offset")
    void process_success_should_ack() {
        consumer.onFotaMessage(record(), acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(dlqService, never()).sendToDlq(any(), anyString());
    }

    @Test
    @DisplayName("契约错误且 DLQ 转存成功才提交 offset")
    void contract_error_dlq_stored_should_ack() {
        doThrow(OtaKafkaMessagingException.nonRecoverable("Kafka Key 与 Envelope.vin 不一致"))
                .when(inboundHandler).processMessage(any());
        when(dlqService.sendToDlq(any(), anyString())).thenReturn(true);

        consumer.onFotaMessage(record(), acknowledgment);

        verify(dlqService).sendToDlq(any(), anyString());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("契约错误但 DLQ 转存失败时不提交 offset 并抛出可恢复异常")
    void contract_error_dlq_failed_should_not_ack() {
        doThrow(OtaKafkaMessagingException.nonRecoverable("Kafka Key 与 Envelope.vin 不一致"))
                .when(inboundHandler).processMessage(any());
        when(dlqService.sendToDlq(any(), anyString())).thenReturn(false);

        OtaKafkaMessagingException thrown = assertThrows(OtaKafkaMessagingException.class,
                () -> consumer.onFotaMessage(record(), acknowledgment));

        assertTrue(thrown.isRecoverable());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("可恢复错误不提交 offset 等待重投")
    void recoverable_error_should_not_ack() {
        doThrow(OtaKafkaMessagingException.recoverable("db down", new RuntimeException("x")))
                .when(inboundHandler).processMessage(any());

        assertThrows(OtaKafkaMessagingException.class,
                () -> consumer.onFotaMessage(record(), acknowledgment));

        verify(acknowledgment, never()).acknowledge();
        verify(dlqService, never()).sendToDlq(any(), anyString());
    }
}
