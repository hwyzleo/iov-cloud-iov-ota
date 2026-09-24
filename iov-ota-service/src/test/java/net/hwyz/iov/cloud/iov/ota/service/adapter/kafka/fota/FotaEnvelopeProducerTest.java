package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.config.OtaKafkaTopicsProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import java.util.List;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FOTA 下行生产者 Topic 测试（CR-020 §7 契约）
 *
 * <p>FotaEnvelopeProducer 只从统一配置 OtaKafkaTopicsProperties.fotaDown 解析
 * 目标 Topic（ota.fota），不散落硬编码字符串。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FotaEnvelopeProducer 生产 Topic（CR-020）")
class FotaEnvelopeProducerTest {

    @Mock private ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    @Mock private KafkaOutboxRepository outboxRepository;
    @Mock private KafkaMessagingMetricsService metrics;
    @Mock private ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatusProvider;

    private OtaKafkaProperties properties;
    private OtaKafkaTopicsProperties topics;
    private FotaEnvelopeProducer producer;

    @BeforeEach
    void setUp() {
        properties = new OtaKafkaProperties();
        topics = new OtaKafkaTopicsProperties();
        producer = new FotaEnvelopeProducer(producerTemplate, outboxRepository, properties, topics,
                metrics, provisioningStatusProvider);
        when(producerTemplate.send(any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("发布到统一配置解析的下行 Topic ota.fota")
    void publishes_to_unified_fota_down_topic() {
        byte[] envelopeBytes = new byte[]{1, 2, 3};
        KafkaOutboxPo po = KafkaOutboxPo.builder()
                .id(1L)
                .payloadType("TASK_CHECK_RESPONSE")
                .vin("LSVAU2188N2ZG4G")
                .envelopeBytes(envelopeBytes)
                .build();
        when(outboxRepository.findPendingReady(anyInt())).thenReturn(List.of(po));
        when(outboxRepository.claim(1L)).thenReturn(true);

        producer.publishPending();

        verify(producerTemplate).send(eq("ota.fota"), eq("LSVAU2188N2ZG4G"), eq(envelopeBytes));
    }

    @Test
    @DisplayName("统一配置被覆盖后发布到覆盖后的 Topic")
    void publishes_to_overridden_topic() {
        topics.setFotaDown("env.ota.fota");
        KafkaOutboxPo po = KafkaOutboxPo.builder()
                .id(2L)
                .payloadType("TASK_CHECK_RESPONSE")
                .vin("LSVAU2188N2ZG4G")
                .envelopeBytes(new byte[]{4, 5})
                .build();
        when(outboxRepository.findPendingReady(anyInt())).thenReturn(List.of(po));
        when(outboxRepository.claim(2L)).thenReturn(true);

        producer.publishPending();

        verify(producerTemplate).send(eq("env.ota.fota"), eq("LSVAU2188N2ZG4G"), any());
    }

    @Test
    @DisplayName("下行生产关闭时不发送")
    void disabled_outbound_skips_publish() {
        properties.getOutbound().setEnabled(false);
        producer.publishPending();
        verify(producerTemplate, never()).send(any(), any(), any());
    }
}
