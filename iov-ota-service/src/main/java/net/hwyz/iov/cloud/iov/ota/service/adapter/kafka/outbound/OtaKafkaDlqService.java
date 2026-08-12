package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

/**
 * OTA Kafka 死信/隔离服务（CR-013 §5/§7）
 *
 * <p>不可恢复契约错误（未知 schema、VIN/device 不一致、Envelope 解析失败等）
 * 原样转存隔离/DLQ topic，供人工可观测与回放。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaKafkaDlqService {

    private final ReactiveKafkaProducerTemplate<String, String> producerTemplate;
    private final OtaKafkaProperties properties;

    /**
     * 将无法处理的 record 原样转存 DLQ topic。
     */
    public void sendToDlq(ConsumerRecord<String, String> record, String reason) {
        if (!properties.getOutbound().isEnabled()) {
            return;
        }
        String dlqTopic = properties.getDlq().getTopic();
        String key = record.key() != null ? record.key() : record.topic();
        producerTemplate.send(dlqTopic, key, record.value())
                .subscribe(
                        result -> log.info("DLQ消息已转存：topic[{}] key[{}] offset[{}] reason[{}]",
                                dlqTopic, key, result.recordMetadata().offset(), reason),
                        error -> log.error("DLQ消息转存失败：topic[{}] reason[{}] error[{}]",
                                dlqTopic, reason, error.getMessage(), error));
    }
}
