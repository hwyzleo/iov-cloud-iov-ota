package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxRepository;
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
import static org.mockito.Mockito.*;

/**
 * 云服务事件 Relay 门禁测试（CR-019 §7 / 对齐 MDM-DSN-CR-034）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CloudEventOutboxRelay Topic Provisioning 门禁（CR-019）")
class CloudEventOutboxRelayTest {

    @Mock private ReactiveKafkaProducerTemplate<String, byte[]> producerTemplate;
    @Mock private CloudEventOutboxRepository outboxRepository;
    @Mock private ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatusProvider;
    @Mock private KafkaTopicProvisioningStatus provisioningStatus;

    private OtaKafkaProperties properties;
    private CloudEventOutboxRelay relay;

    @BeforeEach
    void setUp() {
        properties = new OtaKafkaProperties();
        relay = new CloudEventOutboxRelay(producerTemplate, outboxRepository, properties,
                provisioningStatusProvider);
        when(producerTemplate.send(any(), any(), any()))
                .thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("NOT_READY 暂停本轮：不查询 Outbox、不发送")
    void not_ready_skips_publish() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioningStatus);
        when(provisioningStatus.state()).thenReturn(KafkaTopicProvisioningStatus.State.NOT_READY);

        relay.publishPending();

        verify(outboxRepository, never()).findPendingReady(anyInt());
        verify(producerTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("READY 放行并发布")
    void ready_publishes() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioningStatus);
        when(provisioningStatus.state()).thenReturn(KafkaTopicProvisioningStatus.State.READY);
        CloudEventOutboxPo po = CloudEventOutboxPo.builder()
                .id(1L).eventType("VEHICLE_INVENTORY_OBSERVED")
                .businessKey("VEHICLE_INVENTORY_OBSERVED:abc")
                .payloadJson("{}").vin("LSVAU2188N2ZG4G").build();
        when(outboxRepository.findPendingReady(anyInt())).thenReturn(List.of(po));
        when(outboxRepository.claim(1L)).thenReturn(true);

        relay.publishPending();

        verify(producerTemplate).send(any(), any(), any());
        // 不验证异步回调副作用（subscribe 结果不可控），只验证发送被调用
    }

    @Test
    @DisplayName("未装配 Provisioning（兼容路径）：放行")
    void missing_provisioning_publishes() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(null);
        CloudEventOutboxPo po = CloudEventOutboxPo.builder()
                .id(2L).eventType("VEHICLE_INVENTORY_OBSERVED")
                .businessKey("VEHICLE_INVENTORY_OBSERVED:def")
                .payloadJson("{}").vin("LSVAU2188N2ZG4G").build();
        when(outboxRepository.findPendingReady(anyInt())).thenReturn(List.of(po));
        when(outboxRepository.claim(2L)).thenReturn(true);

        relay.publishPending();

        verify(producerTemplate).send(any(), any(), any());
    }
}
