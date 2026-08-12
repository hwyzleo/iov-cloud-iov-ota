package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.kafka.OtaKafkaProperties;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CR-013 下行消息生产者单元测试：认领、发布、指数退避重试、死信
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OtaKafkaMessageProducer - Outbox 可靠生产")
class OtaKafkaMessageProducerTest {

    @Mock private ReactiveKafkaProducerTemplate<String, String> producerTemplate;
    @Mock private KafkaOutboxRepository outboxRepository;
    @Mock private SenderResult<Void> senderResult;

    private OtaKafkaProperties properties;
    private OtaKafkaMessageProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaOutboxPo outboxPo;

    @BeforeEach
    void setUp() {
        properties = new OtaKafkaProperties();
        properties.getOutbound().setTopic("iov.vagw.down.fota");
        properties.getOutbound().setMaxRetry(3);
        properties.getOutbound().setBackoffBaseSeconds(1);
        properties.getOutbound().setBatchSize(10);
        producer = new OtaKafkaMessageProducer(producerTemplate, outboxRepository, properties,
                new KafkaMessagingMetricsService(), objectMapper);

        outboxPo = KafkaOutboxPo.builder()
                .id(1L)
                .aggregateType("VEHICLE_TASK")
                .aggregateId("10")
                .messageType(OtaMessageType.EXECUTION_PERMITTED.getValue())
                .messageKey("VIN001")
                .correlationId("corr-1")
                .vin("VIN001")
                .payloadJson("{\"executionId\":1001}")
                .retryCount(0)
                .build();

        when(outboxRepository.findPendingReady(anyInt())).thenReturn(List.of(outboxPo));
        when(outboxRepository.claim(1L)).thenReturn(true);
        when(senderResult.recordMetadata()).thenReturn(
                new RecordMetadata(new TopicPartition("iov.vagw.down.fota", 0), 0L, 0L, 0L, 0L, 0, 0));
    }

    @Test
    @DisplayName("发布成功：认领后生产到下行 topic 并标记 PUBLISHED")
    void publishSuccess_marksPublished() {
        when(producerTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(senderResult));

        producer.publishPending();

        verify(producerTemplate).send(eq("iov.vagw.down.fota"), eq("VIN001"), anyString());
        verify(outboxRepository).markPublished(1L);
        verify(outboxRepository, never()).markFailed(any(), any(), anyLong());
        verify(outboxRepository, never()).markDead(any(), any());
    }

    @Test
    @DisplayName("发布失败：指数退避写入下次重试时间")
    void publishFailure_schedulesRetry() {
        when(producerTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("broker down")));

        producer.publishPending();

        // 第 1 次失败 → 标记 FAILED 并退避 1s
        verify(outboxRepository).markFailed(eq(1L), anyString(), eq(1L));
        verify(outboxRepository, never()).markDead(any(), any());
    }

    @Test
    @DisplayName("超过最大重试次数转死信，不丢弃领域事实")
    void exceedMaxRetry_marksDead() {
        when(producerTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("broker down")));
        // 已重试到上限（retry_count = maxRetry - 1）
        outboxPo.setRetryCount(2);

        producer.publishPending();

        verify(outboxRepository, never()).markFailed(any(), any(), anyLong());
        verify(outboxRepository).markDead(eq(1L), anyString());
    }

    @Test
    @DisplayName("组装 Envelope 含 messageType/schemaVersion/payloadDigest/correlationId")
    void envelopeComposition() {
        when(producerTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(senderResult));

        producer.publishPending();

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(producerTemplate).send(anyString(), anyString(), valueCaptor.capture());
        String value = valueCaptor.getValue();
        assertTrue(value.contains("\"messageType\":\"ota.execution.permitted\""));
        assertTrue(value.contains("\"schemaVersion\":1"));
        assertTrue(value.contains("\"payloadDigest\":\"sha256:"));
        assertTrue(value.contains("\"correlationId\":\"corr-1\""));
    }
}
