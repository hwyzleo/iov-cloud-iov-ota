package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound.OtaKafkaDlqService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * FOTA 上行 Envelope 消费者（CR-014 §4.1）
 *
 * <p>监听 iov.vagw.up.fota，value=完整序列化 VehicleMessageEnvelope bytes、Key=VIN。
 * 使用 MANUAL ack：业务成功才提交 offset；可恢复异常不提交（由 Kafka 重投）；
 * 不可恢复契约错误转 DLQ/隔离并正常提交（消息已被技术消费并留存）。
 * Kafka Header 仅用于观测，不参与 schema 或业务决策。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.kafka.inbound.enabled", havingValue = "true", matchIfMissing = true)
public class FotaEnvelopeConsumer {

    private final FotaKafkaInboundHandler inboundHandler;
    private final OtaKafkaDlqService dlqService;
    private final KafkaMessagingMetricsService metrics;

    @KafkaListener(
            topics = "${ota.kafka.inbound.topics:iov.vagw.up.fota}",
            groupId = "${ota.kafka.inbound.group-id:iov-cloud-iov-ota}",
            containerFactory = "fotaKafkaListenerContainerFactory",
            concurrency = "${ota.kafka.inbound.concurrency:3}"
    )
    public void onFotaMessage(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) {
        log.info("收到 FOTA 上行 Envelope: topic={}, key={}, partition={}, offset={}",
                record.topic(), FotaEnvelopeValidator.maskVin(record.key()), record.partition(), record.offset());
        try {
            inboundHandler.processMessage(record);
        } catch (OtaKafkaMessagingException e) {
            if (e.isRecoverable()) {
                metrics.increment(KafkaMessagingMetricsService.INBOX_FAILED);
                log.error("FOTA 消息可恢复处理失败，不提交 offset 等待重投: offset={}, error={}",
                        record.offset(), e.getMessage(), e);
                throw e;
            }
            // 不可恢复契约错误：转 DLQ/隔离，正常提交（消息已留存）
            metrics.increment(KafkaMessagingMetricsService.INBOX_DLQ);
            log.error("FOTA 消息不可恢复契约错误，转 DLQ: offset={}, error={}",
                    record.offset(), e.getMessage());
            dlqService.sendToDlq(record, e.getMessage());
        } catch (Exception e) {
            metrics.increment(KafkaMessagingMetricsService.INBOX_FAILED);
            log.error("FOTA 消息处理异常，不提交 offset 等待重投: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            throw e;
        }
        acknowledgment.acknowledge();
        log.info("FOTA 上行 Envelope 处理并提交 offset: topic={}, offset={}", record.topic(), record.offset());
    }
}
