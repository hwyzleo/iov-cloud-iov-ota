package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.TaskCheckCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Types.InventoryMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FOTA 入站端到端集成测试（CR-014 §4.2、验收 2/4）
 *
 * <p>真实入站流水线（validator + router + handler + outbox appender），验证：
 * 上行 raw Envelope bytes → 强类型解析 → 领域命令 → 下行 RESPONSE Envelope bytes 冻结入 Outbox；
 * Inbox (message_id, sha256) 幂等复用与摘要冲突隔离。
 *
 * @author hwyz_leo
 */
@DisplayName("FOTA Kafka 入站集成测试 - Envelope 幂等与响应冻结")
class FotaKafkaInboundIntegrationTest {

    private FotaKafkaInboundHandler inboundHandler;
    private KafkaInboxRepository inboxRepository;
    private KafkaOutboxRepository outboxRepository;
    private TaskCheckCommandHandler commandHandler;
    private final List<KafkaOutboxPo> appended = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(new ObjectMapper());
        guard.verify();
        FotaEnvelopeFactory factory = new FotaEnvelopeFactory(guard);
        FotaEnvelopeValidator validator = new FotaEnvelopeValidator(guard);

        inboxRepository = mock(KafkaInboxRepository.class);
        outboxRepository = mock(KafkaOutboxRepository.class);
        commandHandler = mock(TaskCheckCommandHandler.class);
        KafkaMessagingMetricsService metrics = mock(KafkaMessagingMetricsService.class);

        // 捕获 Outbox 追加
        when(outboxRepository.append(any())).thenAnswer(inv -> {
            KafkaOutboxPo po = inv.getArgument(0);
            po.setId((long) (appended.size() + 1));
            appended.add(po);
            return po.getId();
        });
        FotaOutboxAppender outboxAppender = new FotaOutboxAppender(outboxRepository);

        // registry 要求全部 INBOUND 类型注册 handler（fail-closed），此处注册全部 12 个（仅 TaskCheck 被实际执行）
        List<FotaPayloadHandler<?>> handlers = List.of(
                new TaskCheckPayloadHandler(commandHandler, factory, outboxAppender, metrics),
                new ConsentPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ConsentCommandHandler.class), factory, outboxAppender, metrics),
                new DownloadGrantPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.DownloadGrantCommandHandler.class), factory, outboxAppender, metrics),
                new StageResultPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.StageResultCommandHandler.class), factory, outboxAppender, metrics),
                new InstallPermitPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.InstallPermitCommandHandler.class), factory, outboxAppender, metrics),
                new ExecutionEventPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ExecutionEventCommandHandler.class), factory, outboxAppender, metrics),
                new ControlAckPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ControlAckCommandHandler.class), factory, outboxAppender, metrics),
                new FinalResultPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FinalResultCommandHandler.class), factory, outboxAppender, metrics),
                new LogGrantPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.LogGrantCommandHandler.class), factory, outboxAppender, metrics),
                new LogResultPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.LogResultCommandHandler.class), factory, outboxAppender, metrics),
                new ReconcilePayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ReconcileCommandHandler.class), factory, outboxAppender, metrics),
                new PolicyPayloadHandler(mock(net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.PolicyCommandHandler.class), factory, outboxAppender, metrics));
        FotaPayloadRouter router = new FotaPayloadRouter(guard, handlers);
        router.init();

        inboundHandler = new FotaKafkaInboundHandler(validator, router, inboxRepository, metrics);
    }

    @Test
    @DisplayName("上行 Envelope → 强类型处理 → 下行 RESPONSE Envelope bytes 冻结入 Outbox")
    void process_appends_frozen_response_envelope() throws InvalidProtocolBufferException {
        when(inboxRepository.selectForUpdate(any(), any())).thenReturn(null);
        when(commandHandler.handle(any(), any())).thenReturn(TaskCheckResponse.newBuilder()
                .setNextAction("PROCEED").build());

        VehicleMessageEnvelope envelope = requestEnvelope();
        inboundHandler.processMessage(record(envelope));

        // Inbox 已标记处理
        verify(inboxRepository).markProcessed(argThat(po ->
                "vehicle.fota.v1.TaskCheckRequest".equals(po.getPayloadType())
                        && "msg-1".equals(po.getMessageId())
                        && FotaDigests.sha256(envelope.toByteArray()).equals(po.getEnvelopeSha256())
                        && po.getStatus().equals(KafkaInboxPo.STATUS_PROCESSED)));
        // Outbox 冻结响应 Envelope bytes，可解析为 TaskCheckResponse
        assertEquals(1, appended.size());
        VehicleMessageEnvelope down = VehicleMessageEnvelope.parseFrom(appended.get(0).getEnvelopeBytes());
        assertEquals("RESPONSE", appended.get(0).getMessageKind());
        assertEquals("msg-1", down.getCorrelationId());
        assertEquals("vehicle.fota.v1.TaskCheckResponse", down.getPayloadType());
        assertEquals("PROCEED", TaskCheckResponse.parseFrom(down.getPayload()).getNextAction());
    }

    @Test
    @DisplayName("同 message_id 同摘要：幂等复用，不再执行业务")
    void duplicate_same_hash_is_idempotent() {
        VehicleMessageEnvelope envelope = requestEnvelope();
        KafkaInboxPo existing = KafkaInboxPo.builder()
                .consumerName("vehicle.fota.v1.TaskCheckRequest")
                .messageId("msg-1")
                .envelopeSha256(FotaDigests.sha256(envelope.toByteArray()))
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .build();
        when(inboxRepository.selectForUpdate(any(), any())).thenReturn(existing);

        inboundHandler.processMessage(record(envelope));

        verify(commandHandler, never()).handle(any(), any());
        verify(inboxRepository, never()).markProcessed(any());
        assertTrue(appended.isEmpty(), "幂等复用不应追加 Outbox");
    }

    @Test
    @DisplayName("同 message_id 异摘要：冲突隔离并追加业务拒绝响应")
    void duplicate_different_hash_conflicts() {
        VehicleMessageEnvelope envelope = requestEnvelope();
        KafkaInboxPo existing = KafkaInboxPo.builder()
                .consumerName("vehicle.fota.v1.TaskCheckRequest")
                .messageId("msg-1")
                .envelopeSha256(FotaDigests.sha256("different-bytes".getBytes()))
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .build();
        when(inboxRepository.selectForUpdate(any(), any())).thenReturn(existing);

        inboundHandler.processMessage(record(envelope));

        verify(inboxRepository).markConflict(any(), any(), any());
        verify(commandHandler, never()).handle(any(), any());
        // 冲突 → handleConflict 追加拒绝 RESPONSE
        assertEquals(1, appended.size());
        assertEquals("RESPONSE", appended.get(0).getMessageKind());
    }

    // ------------------------------------------------------------------ helpers

    private static VehicleMessageEnvelope requestEnvelope() {
        TaskCheckRequest payload = TaskCheckRequest.newBuilder()
                .setInventoryMode(InventoryMode.INVENTORY_MODE_DIGEST)
                .setInventoryRevision(3L)
                .build();
        return VehicleMessageEnvelope.newBuilder()
                .setRequestId("req-1")
                .setTimestampMs(System.currentTimeMillis())
                .setProtocolVersion("fota-v1")
                .setDeviceId("dev-1")
                .setVin("LSVAU2188N2ZG4G")
                .setMessageId("msg-1")
                .setMessageKind(MessageKind.MESSAGE_KIND_REQUEST)
                .setService("vehicle.fota")
                .setPayloadType("vehicle.fota.v1.TaskCheckRequest")
                .setPayload(payload.toByteString())
                .build();
    }

    private static ConsumerRecord<String, byte[]> record(VehicleMessageEnvelope envelope) {
        return new ConsumerRecord<>("iov.vagw.up.fota", 0, 0L, envelope.getVin(), envelope.toByteArray());
    }
}
