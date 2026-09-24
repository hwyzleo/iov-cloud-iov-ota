package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicsProperties;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

/**
 * FOTA 死信/隔离服务（CR-014 §9 / CR-020 §4.3）
 *
 * <p>不可恢复契约错误（Envelope 不可解析、registry 漂移、VIN/service/kind/TTL 非法等）
 * 原样转存隔离/DLQ topic（value 保持 raw bytes），供人工可观测与回放。
 * 目标 Topic 为 ota.fota.dlq.up（CR-020，配置键 ota.kafka.topics.fota-up-dlq），
 * 由 IOV-OTA 生产并幂等初始化；与 VAGW 生产的消费流 vagw.fota.dlq.up 不得混用。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaKafkaDlqService {

    private final ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    private final OtaKafkaProperties properties;
    private final OtaKafkaTopicsProperties topics;

    /**
     * 将无法处理的 record 原样转存 DLQ topic（raw bytes 不变）。
     */
    public void sendToDlq(ConsumerRecord<String, byte[]> record, String reason) {
        if (!properties.getOutbound().isEnabled()) {
            return;
        }
        String dlqTopic = topics.getFotaUpDlq();
        String key = record.key() != null ? record.key() : record.topic();
        producerTemplate.send(dlqTopic, key, record.value())
                .subscribe(
                        result -> log.info("DLQ 消息已转存：topic[{}] key[{}] offset[{}] reason[{}]",
                                dlqTopic, FotaEnvelopeValidator.maskVin(key),
                                result.recordMetadata().offset(), reason),
                        error -> log.error("DLQ 消息转存失败：topic[{}] reason[{}] error[{}]",
                                dlqTopic, reason, error.getMessage(), error));
    }
}
