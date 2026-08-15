package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound.OtaKafkaDlqService;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.delivery.DeliveryObservationService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import vagw.v1.Delivery.GatewayDeliveryStatus;

/**
 * GatewayDeliveryStatus 独立消费者（CR-014 §7.1）
 *
 * <p>独立消费 iov.vagw.delivery.fota（Key=VIN，value=serialized vagw.v1.GatewayDeliveryStatus）；
 * 使用独立 codec、router 与状态语义，不经过 FotaEnvelopeConsumer 或 PayloadType Router。
 * MANUAL ack：业务成功才提交 offset；不可恢复契约错误转 DLQ/隔离。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.kafka.delivery.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayDeliveryStatusConsumer {

    private final GatewayDeliveryStatusValidator validator;
    private final DeliveryObservationService observationService;
    private final DeliveryObservationAssembler assembler;
    private final OtaKafkaDlqService dlqService;
    private final OtaKafkaProperties properties;
    private final KafkaMessagingMetricsService metrics;

    @KafkaListener(
            topics = "${ota.kafka.delivery.topic:iov.vagw.delivery.fota}",
            groupId = "${ota.kafka.delivery.group-id:iov-cloud-iov-ota-delivery}",
            containerFactory = "fotaKafkaListenerContainerFactory",
            concurrency = "${ota.kafka.delivery.concurrency:2}"
    )
    public void onDeliveryStatus(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) {
        log.info("收到技术投递状态：topic={}, key={}, partition={}, offset={}",
                record.topic(), maskVin(record.key()), record.partition(), record.offset());
        try {
            GatewayDeliveryStatus status = validator.validate(record);
            observationService.record(status);
            DeliveryObservationAssembler.Summary s = assembler.summarize(status);
            log.info("技术投递观测：messageId[{}] vin[{}] stage[{}] outcome[{}] reason[{}] retryable[{}] latency[{}ms]",
                    s.messageId(), s.vinMasked(), s.stage(), s.outcome(), s.reason(), s.retryable(), s.latencyMs());
        } catch (OtaKafkaMessagingException e) {
            if (e.isRecoverable()) {
                metrics.increment(KafkaMessagingMetricsService.INBOX_FAILED);
                log.error("技术投递可恢复处理失败，不提交 offset 等待重投: offset={}, error={}",
                        record.offset(), e.getMessage(), e);
                throw e;
            }
            metrics.increment(KafkaMessagingMetricsService.INBOX_DLQ);
            log.error("技术投递不可恢复契约错误，转 DLQ: offset={}, error={}",
                    record.offset(), e.getMessage());
            dlqService.sendToDlq(record, e.getMessage());
        } catch (Exception e) {
            metrics.increment(KafkaMessagingMetricsService.INBOX_FAILED);
            log.error("技术投递处理异常，不提交 offset 等待重投: offset={}, error={}",
                    record.offset(), e.getMessage(), e);
            throw e;
        }
        acknowledgment.acknowledge();
        log.info("技术投递状态处理并提交 offset: topic={}, offset={}", record.topic(), record.offset());
    }

    private static String maskVin(String vin) {
        if (vin == null || vin.length() <= 4) {
            return "***";
        }
        return "***" + vin.substring(vin.length() - 4);
    }
}
