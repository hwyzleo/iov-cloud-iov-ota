package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Kafka Inbox 毒消息恢复器（DefaultErrorHandler 退避耗尽后的兜底）。
 *
 * <p>有界退避重试全部失败后，必须让分区继续推进，同时保证消息不丢失：
 * 先把原始 bytes + 元数据落盘隔离（可人工回放），再提交 offset。落盘失败时
 * ERROR 告警并放弃该消息（比永久阻塞分区更可控，且已有指标可观测）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaQuarantineRecoverer implements ConsumerRecordRecoverer {

    private final KafkaQuarantineStore store;
    private final KafkaMessagingMetricsService metrics;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception ex) {
        metrics.increment(KafkaMessagingMetricsService.INBOX_QUARANTINED);
        try {
            Path file = store.store(record, ex);
            log.error("消息退避重试耗尽，已落盘隔离: topic={}, partition={}, offset={}, file={}, error={}",
                    record.topic(), record.partition(), record.offset(), file, ex.getMessage());
        } catch (Exception e) {
            log.error("消息退避重试耗尽且隔离落盘失败（消息将丢失，需人工对账）: "
                            + "topic={}, partition={}, offset={}, error={}",
                    record.topic(), record.partition(), record.offset(), ex.getMessage(), e);
        }
    }
}
