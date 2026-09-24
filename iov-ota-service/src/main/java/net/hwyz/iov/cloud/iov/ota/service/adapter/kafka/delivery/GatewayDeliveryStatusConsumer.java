package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound.OtaKafkaDlqService;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.delivery.DeliveryObservationService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import vagw.v1.Delivery.GatewayDeliveryStatus;

/**
 * GatewayDeliveryStatus 独立消费者（CR-014 §7.1 / CR-020 §4.5）
 *
 * <p>CR-020 起默认停用：Kafka Topic 目录未登记独立 delivery Topic，IOV-OTA 不创建、
 * 不订阅该流，也不将 GatewayDeliveryStatus 混入 vagw.fota。若需恢复，必须先完成
 * 目录与契约治理（新增正式 Topic 记录并通过配套协议/ACL CR 明确 value、生产者和消费者），
 * 再显式开启 ota.kafka.delivery.enabled=true 并配置 topic。
 *
 * <p>技术投递结果不推进 Task/VehicleTask/Execution 成功；正式 FOTA RESPONSE/EVENT
 * 与领域状态继续作为业务结果来源。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ota.kafka.delivery.enabled", havingValue = "true", matchIfMissing = false)
public class GatewayDeliveryStatusConsumer {

    private final GatewayDeliveryStatusValidator validator;
    private final DeliveryObservationService observationService;
    private final DeliveryObservationAssembler assembler;
    private final OtaKafkaDlqService dlqService;
    private final KafkaMessagingMetricsService metrics;

    @KafkaListener(
            topics = "${ota.kafka.delivery.topic:}",
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
