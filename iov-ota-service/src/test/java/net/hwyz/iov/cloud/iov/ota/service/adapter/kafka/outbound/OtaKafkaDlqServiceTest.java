package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicsProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FOTA 上行 DLQ 服务 Topic 测试（CR-020 §7 契约）
 *
 * <p>OtaKafkaDlqService 只从统一配置 OtaKafkaTopicsProperties.fotaUpDlq 解析
 * 目标 Topic（ota.fota.dlq.up），与 VAGW 生产的消费流 vagw.fota.dlq.up 不混用。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OtaKafkaDlqService 生产 Topic（CR-020）")
class OtaKafkaDlqServiceTest {

    @Mock private ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;

    private OtaKafkaProperties properties;
    private OtaKafkaTopicsProperties topics;
    private OtaKafkaDlqService dlqService;

    @BeforeEach
    void setUp() {
        properties = new OtaKafkaProperties();
        topics = new OtaKafkaTopicsProperties();
        dlqService = new OtaKafkaDlqService(producerTemplate, properties, topics);
        when(producerTemplate.send(any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("不可恢复错误转存到统一配置解析的 DLQ Topic ota.fota.dlq.up")
    void sends_to_unified_fota_up_dlq_topic() {
        byte[] raw = new byte[]{9, 8, 7};
        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("vagw.fota", 0, 42L, "LSVAU2188N2ZG4G", raw);

        dlqService.sendToDlq(record, "Envelope 解析失败");

        verify(producerTemplate).send(eq("ota.fota.dlq.up"), eq("LSVAU2188N2ZG4G"), eq(raw));
    }

    @Test
    @DisplayName("Key 为空时以来源 Topic 作为 Key")
    void falls_back_to_source_topic_as_key() {
        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("vagw.fota", 1, 7L, null, new byte[]{1});

        dlqService.sendToDlq(record, "contract error");

        verify(producerTemplate).send(eq("ota.fota.dlq.up"), eq("vagw.fota"), any());
    }

    @Test
    @DisplayName("下行生产关闭时不转存")
    void disabled_outbound_skips_dlq() {
        properties.getOutbound().setEnabled(false);
        dlqService.sendToDlq(new ConsumerRecord<>("vagw.fota", 0, 0L, "K", new byte[]{1}), "r");
        verify(producerTemplate, never()).send(any(), any(), any());
    }
}
