package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 毒消息恢复器测试：隔离落盘 + 指标计数；落盘失败不向容器抛出异常。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaQuarantineRecoverer")
class KafkaQuarantineRecovererTest {

    @Mock private KafkaQuarantineStore store;

    private KafkaMessagingMetricsService metrics;
    private KafkaQuarantineRecoverer recoverer;

    @BeforeEach
    void setUp() {
        metrics = new KafkaMessagingMetricsService();
        recoverer = new KafkaQuarantineRecoverer(store, metrics);
    }

    private static ConsumerRecord<String, byte[]> record() {
        return new ConsumerRecord<>("vagw.fota", 0, 5L, "VIN-K", new byte[]{1});
    }

    @Test
    @DisplayName("重试耗尽后落盘隔离并计数")
    void stores_and_counts() {
        when(store.store(any(), any())).thenReturn(Path.of("/tmp/q.bin"));

        recoverer.accept(record(), new IllegalStateException("boom"));

        verify(store).store(any(), any());
        assertEquals(1L, metrics.snapshot()
                .get(KafkaMessagingMetricsService.INBOX_QUARANTINED));
    }

    @Test
    @DisplayName("落盘失败不向上抛出（分区必须继续推进）")
    void store_failure_does_not_propagate() {
        when(store.store(any(), any())).thenThrow(new RuntimeException("disk full"));

        assertDoesNotThrow(() -> recoverer.accept(record(), new IllegalStateException("boom")));
        assertEquals(1L, metrics.snapshot()
                .get(KafkaMessagingMetricsService.INBOX_QUARANTINED));
    }
}
