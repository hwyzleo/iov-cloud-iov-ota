package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.OtaKafkaMessageHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.router.OtaMessageRouter;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageSchemaRegistry;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CR-013 入站流水线单元测试：幂等、摘要冲突、契约错误
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OtaKafkaInboundHandler - 消费事务与 Inbox 去重")
class OtaKafkaInboundHandlerTest {

    @Mock private OtaMessageSchemaRegistry schemaRegistry;
    @Mock private OtaMessageRouter router;
    @Mock private KafkaInboxRepository inboxRepository;
    @Mock private OtaKafkaMessageHandler handler;

    private OtaKafkaInboundHandler inboundHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OtaEnvelopeValidator envelopeValidator = new OtaEnvelopeValidator();
    private final KafkaMessagingMetricsService metrics = new KafkaMessagingMetricsService();

    private ConsumerRecord<String, String> record;

    @BeforeEach
    void setUp() throws Exception {
        inboundHandler = new OtaKafkaInboundHandler(
                envelopeValidator, schemaRegistry, router, inboxRepository, metrics, objectMapper);

        when(schemaRegistry.supports(anyString(), any())).thenReturn(true);
        when(router.resolve(anyString())).thenReturn(handler);
        when(handler.messageType()).thenReturn(OtaMessageType.TASK_DETECT_REQUESTED);
        when(handler.businessKey(any(), any())).thenReturn("bk-001");
        when(handler.handle(any(), any())).thenReturn(100L);
        when(handler.handleConflict(any(), any(), anyString())).thenReturn(200L);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("inventoryMode", "FULL");
        OtaKafkaEnvelope envelope = OtaKafkaEnvelope.builder()
                .messageId("msg-001")
                .messageType(OtaMessageType.TASK_DETECT_REQUESTED.getValue())
                .schemaVersion(1)
                .timestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .deviceId("CGW-001")
                .vin("VIN001")
                .correlationId("corr-001")
                .payloadDigest("sha256:digest-1")
                .payload(payload)
                .build();
        record = new ConsumerRecord<>("iov.vagw.up.fota", 0, 1L, "VIN001",
                objectMapper.writeValueAsString(envelope));
    }

    @Test
    @DisplayName("首次消息：调用处理器并写入 Inbox 处理结果索引")
    void firstMessage_processesAndRecordsInbox() {
        when(inboxRepository.selectForUpdate(anyString(), anyString())).thenReturn(null);

        assertDoesNotThrow(() -> inboundHandler.processMessage(record));

        verify(handler).handle(any(), any());
        verify(inboxRepository).markProcessed(argThat(po ->
                po.getConsumerName().equals(OtaMessageType.TASK_DETECT_REQUESTED.getValue())
                        && po.getStatus().equals(KafkaInboxPo.STATUS_PROCESSED)
                        && po.getResultMessageId() != null
                        && po.getKafkaOffset() == 1L));
        verify(handler, never()).handleConflict(any(), any(), anyString());
    }

    @Test
    @DisplayName("重复同摘要：幂等跳过，不重复推进领域状态")
    void duplicateSameDigest_idempotentSkip() {
        KafkaInboxPo existing = KafkaInboxPo.builder()
                .consumerName(OtaMessageType.TASK_DETECT_REQUESTED.getValue())
                .businessKey("bk-001")
                .payloadDigest("sha256:digest-1")
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .resultMessageId(100L)
                .build();
        when(inboxRepository.selectForUpdate(anyString(), anyString())).thenReturn(existing);

        assertDoesNotThrow(() -> inboundHandler.processMessage(record));

        verify(handler, never()).handle(any(), any());
        verify(inboxRepository, never()).markProcessed(any());
    }

    @Test
    @DisplayName("重复异摘要：生产冲突拒绝事件并记录 CONFLICT")
    void duplicateDifferentDigest_conflictResult() {
        KafkaInboxPo existing = KafkaInboxPo.builder()
                .consumerName(OtaMessageType.TASK_DETECT_REQUESTED.getValue())
                .businessKey("bk-001")
                .payloadDigest("sha256:old-digest")
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .resultMessageId(100L)
                .build();
        when(inboxRepository.selectForUpdate(anyString(), anyString())).thenReturn(existing);

        assertDoesNotThrow(() -> inboundHandler.processMessage(record));

        verify(handler).handleConflict(any(), any(), anyString());
        verify(inboxRepository).markResult(anyString(), anyString(), eq(KafkaInboxPo.STATUS_CONFLICT),
                eq(200L), anyString());
        verify(handler, never()).handle(any(), any());
    }

    @Test
    @DisplayName("不支持的 schema：不可恢复契约错误")
    void unsupportedSchema_nonRecoverable() {
        when(schemaRegistry.supports(anyString(), any())).thenReturn(false);
        OtaKafkaMessagingException e = assertThrows(OtaKafkaMessagingException.class,
                () -> inboundHandler.processMessage(record));
        assertFalse(e.isRecoverable());
        verify(handler, never()).handle(any(), any());
    }

    @Test
    @DisplayName("缺少 VIN：不可恢复契约错误")
    void missingVin_nonRecoverable() throws Exception {
        OtaKafkaEnvelope envelope = objectMapper.readValue(record.value(), OtaKafkaEnvelope.class);
        envelope.setVin(null);
        ConsumerRecord<String, String> bad = new ConsumerRecord<>("iov.vagw.up.fota", 0, 2L, "k",
                objectMapper.writeValueAsString(envelope));
        OtaKafkaMessagingException e = assertThrows(OtaKafkaMessagingException.class,
                () -> inboundHandler.processMessage(bad));
        assertFalse(e.isRecoverable());
        verify(handler, never()).handle(any(), any());
    }

    @Test
    @DisplayName("业务异常传播（可恢复，由消费方不提交 offset）")
    void businessException_propagates() {
        when(inboxRepository.selectForUpdate(anyString(), anyString())).thenReturn(null);
        doThrow(new IllegalStateException("车辆任务不存在")).when(handler).handle(any(), any());

        assertThrows(IllegalStateException.class, () -> inboundHandler.processMessage(record));
        verify(inboxRepository, never()).markProcessed(any());
    }
}
