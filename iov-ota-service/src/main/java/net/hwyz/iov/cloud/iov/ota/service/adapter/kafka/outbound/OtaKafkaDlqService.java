package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicsProperties;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota.FotaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * FOTA 死信/隔离服务（CR-014 §9 / CR-020 §4.3）
 *
 * <p>不可恢复契约错误（Envelope 不可解析、registry 漂移、VIN/service/kind/TTL 非法等）
 * 原样转存隔离/DLQ topic（value 保持 raw bytes），供人工可观测与回放。
 * 目标 Topic 为 ota.fota.dlq.up（CR-020，配置键 ota.kafka.topics.fota-up-dlq），
 * 由 IOV-OTA 生产并幂等初始化；与 VAGW 生产的消费流 vagw.fota.dlq.up 不得混用。
 *
 * <p>转存采用同步等待（有界超时）并返回结果：调用方必须在转存成功后才提交 offset，
 * 否则「DLQ 转存失败 + offset 已提交」会导致契约错误消息被静默丢弃、不可回放。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaKafkaDlqService {

    /** 单条 DLQ 转存的等待上限（秒）：超出视为转存失败，交由调用方保留 offset 重投 */
    private static final long DLQ_SEND_TIMEOUT_SECONDS = 5;

    private final ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    private final OtaKafkaProperties properties;
    private final OtaKafkaTopicsProperties topics;

    /**
     * 将无法处理的 record 原样转存 DLQ topic（raw bytes 不变），同步等待结果。
     *
     * @return true 表示已可靠转存（或下行生产显式关闭）；false 表示转存失败，
     *         调用方不得提交 offset
     */
    public boolean sendToDlq(ConsumerRecord<String, byte[]> record, String reason) {
        if (!properties.getOutbound().isEnabled()) {
            log.warn("下行生产已关闭，DLQ 消息未转存：topic[{}] key[{}] offset[{}] reason[{}]",
                    record.topic(), FotaEnvelopeValidator.maskVin(record.key()), record.offset(), reason);
            return true;
        }
        String dlqTopic = topics.getFotaUpDlq();
        String key = record.key() != null ? record.key() : record.topic();
        try {
            var result = producerTemplate.send(dlqTopic, key, record.value())
                    .block(Duration.ofSeconds(DLQ_SEND_TIMEOUT_SECONDS));
            if (result == null || result.recordMetadata() == null) {
                log.error("DLQ 消息未确认转存：topic[{}] key[{}] offset[{}] reason[{}]",
                        dlqTopic, FotaEnvelopeValidator.maskVin(key), record.offset(), reason);
                return false;
            }
            log.info("DLQ 消息已转存：topic[{}] key[{}] sourceOffset[{}] dlqOffset[{}] reason[{}]",
                    dlqTopic, FotaEnvelopeValidator.maskVin(key), record.offset(),
                    result.recordMetadata().offset(), reason);
            return true;
        } catch (Exception e) {
            log.error("DLQ 消息转存失败：topic[{}] key[{}] sourceOffset[{}] reason[{}] error[{}]",
                    dlqTopic, FotaEnvelopeValidator.maskVin(key), record.offset(), reason, e.getMessage(), e);
            return false;
        }
    }
}
