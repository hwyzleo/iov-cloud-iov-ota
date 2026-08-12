package net.hwyz.iov.cloud.iov.ota.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.OtaKafkaInboundHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.ExecutionEventReportedHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.ExecutionPermitRequestedHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.ExecutionResultReportedHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.RecoveryRequestedHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.router.OtaMessageRouter;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaEnvelopeValidator;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageSchemaRegistry;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.RecoveryAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InstallPermitService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.KafkaInboxMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.KafkaOutboxMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlAckMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.LocalPermitTokenSigner;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryVehicleTaskRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * CR-013 Kafka 入站端到端集成测试（T-6.x）
 *
 * <p>真实入站流水线 + 真实处理器 + 真实应用服务（内存领域仓库）：
 * 安装许可 → 事件 → 收口 → 恢复全链路；验证 Inbox 幂等（重复/重放不重复推进领域状态）、
 * 领域状态与 Kafka Outbox 同事务产出下行结果消息。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CR-013 Kafka 入站集成测试 - 执行全链路")
class OtaKafkaInboundIntegrationTest {

    @Mock private ExecutionEcuResultMapper executionEcuResultMapper;
    @Mock private ExecutionEventMapper executionEventMapper;
    @Mock private ExecutionControlMapper executionControlMapper;
    @Mock private ExecutionControlAckMapper executionControlAckMapper;
    @Mock private KafkaInboxMapper kafkaInboxMapper;
    @Mock private KafkaOutboxMapper kafkaOutboxMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final KafkaMessagingMetricsService metrics = new KafkaMessagingMetricsService();

    private InMemoryVehicleTaskRepository vehicleTaskRepository;
    private InMemoryExecutionRepository executionRepository;
    private InMemoryTaskRepository taskRepository;
    private InMemoryOutboxRepository domainOutboxRepository;

    private KafkaInboxRepository inboxRepository;
    private KafkaOutboxRepository kafkaOutboxRepository;
    private OtaKafkaInboundHandler inboundHandler;

    private final Map<String, ExecutionEventPo> eventStore = new HashMap<>();
    private final Map<String, KafkaInboxPo> inboxStore = new HashMap<>();
    private final Map<Long, KafkaOutboxPo> kafkaOutboxStore = new HashMap<>();
    private final List<KafkaOutboxPo> outboxOrder = new ArrayList<>();
    private final java.util.concurrent.atomic.AtomicLong outboxIdSeed = new java.util.concurrent.atomic.AtomicLong(1);

    private final Instant now = Instant.now();
    private final Instant startTime = now.minusSeconds(60);
    private final Instant endTime = now.plusSeconds(3600);

    private Long vehicleTaskId;
    private Long executionId;

    @BeforeEach
    void setUp() {
        // 内存领域仓库
        vehicleTaskRepository = new InMemoryVehicleTaskRepository();
        executionRepository = new InMemoryExecutionRepository();
        taskRepository = new InMemoryTaskRepository();
        domainOutboxRepository = new InMemoryOutboxRepository();

        // 事件存储
        when(executionEventMapper.selectByEventId(any())).thenAnswer(inv -> eventStore.get(inv.getArgument(0)));
        when(executionEventMapper.insert(any(ExecutionEventPo.class))).thenAnswer(inv -> {
            ExecutionEventPo po = inv.getArgument(0);
            eventStore.put(po.getEventId(), po);
            return 1;
        });
        when(executionControlMapper.selectLatestByExecutionId(any())).thenReturn(null);

        // 应用服务
        InstallPermitService installPermitService = new InstallPermitService(new LocalPermitTokenSigner());
        ExecutionAppService executionAppService = new ExecutionAppService(vehicleTaskRepository,
                executionRepository, taskRepository, installPermitService, domainOutboxRepository, executionEcuResultMapper);
        ExecutionEventAppService executionEventAppService = new ExecutionEventAppService(executionRepository,
                executionEventMapper, executionControlMapper, executionControlAckMapper);
        RecoveryAppService recoveryAppService = new RecoveryAppService(vehicleTaskRepository,
                executionRepository, executionControlMapper);

        // Kafka Inbox（内存）
        inboxRepository = new KafkaInboxRepository(kafkaInboxMapper);
        when(kafkaInboxMapper.selectForUpdate(any(), any())).thenAnswer(inv ->
                inboxStore.get(key(inv.getArgument(0), inv.getArgument(1))));
        when(kafkaInboxMapper.insert(any(KafkaInboxPo.class))).thenAnswer(inv -> {
            KafkaInboxPo po = inv.getArgument(0);
            inboxStore.put(key(po.getConsumerName(), po.getBusinessKey()), po);
            return 1;
        });
        when(kafkaInboxMapper.updateProcessResult(any(), any(), any(), any(), any())).thenReturn(1);

        // Kafka Outbox（内存）
        kafkaOutboxRepository = new KafkaOutboxRepository(kafkaOutboxMapper);
        when(kafkaOutboxMapper.insert(any(KafkaOutboxPo.class))).thenAnswer(inv -> {
            KafkaOutboxPo po = inv.getArgument(0);
            po.setId(outboxIdSeed.getAndIncrement());
            kafkaOutboxStore.put(po.getId(), po);
            outboxOrder.add(po);
            return 1;
        });
        when(kafkaOutboxMapper.selectPendingReady(anyInt())).thenReturn(new ArrayList<>());
        when(kafkaOutboxMapper.claim(any())).thenReturn(1);
        when(kafkaOutboxMapper.markPublished(any())).thenReturn(1);

        // 入站流水线
        OtaMessageRouter router = new OtaMessageRouter(List.of(
                new ExecutionPermitRequestedHandler(objectMapper, kafkaOutboxRepository, metrics, executionAppService),
                new ExecutionEventReportedHandler(objectMapper, kafkaOutboxRepository, metrics, executionEventAppService),
                new ExecutionResultReportedHandler(objectMapper, kafkaOutboxRepository, metrics, executionAppService),
                new RecoveryRequestedHandler(objectMapper, kafkaOutboxRepository, metrics, recoveryAppService)
        ));
        inboundHandler = new OtaKafkaInboundHandler(new OtaEnvelopeValidator(),
                new OtaMessageSchemaRegistry(), router, inboxRepository, metrics, objectMapper);

        // 初始状态：已发布任务 + 就绪车辆任务
        Task task = buildReleasedTask();
        VehicleTask vt = buildReadyVehicleTask();
        taskRepository.save(task);
        vehicleTaskRepository.save(vt);
        vehicleTaskId = vt.getId().getValue();
    }

    @Test
    @DisplayName("完整链路：许可→事件→收口→恢复，Kafka 消息驱动并产出下行结果")
    void fullExecutionLifecycle_viaKafkaMessages() throws Exception {
        // 1. 申请安装许可
        consume(permitMessage("idem-001", true));
        assertEquals(VehicleTaskStatus.EXECUTING, vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId)).get().getStatus());
        // 从 outbox 的 permitted 负载中取 executionId
        KafkaOutboxPo permitted = kafkaOutboxStore.values().stream()
                .filter(p -> p.getMessageType().equals("ota.execution.permitted"))
                .findFirst().orElseThrow();
        JsonNode permittedPayload = objectMapper.readTree(permitted.getPayloadJson());
        executionId = permittedPayload.get("executionId").asLong();
        assertEquals("ota.execution.permitted", permitted.getMessageType());
        assertEquals("corr-permit", permitted.getCorrelationId());

        // 2. 连续安装事件
        for (long seq = 1; seq <= 3; seq++) {
            consume(eventMessage(seq, "evt-flow-" + seq));
        }

        // 3. 最终结果收口
        consume(resultMessage("SUCCEEDED", 3L, "idem-result-001"));
        assertTrue(kafkaOutboxStore.values().stream()
                .anyMatch(p -> p.getMessageType().equals("ota.execution.result.acknowledged")));
        assertEquals(VehicleTaskStatus.SUCCEEDED, vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId)).get().getStatus());

        // 4. 恢复查询
        consume(recoveryMessage());
        assertTrue(kafkaOutboxStore.values().stream()
                .anyMatch(p -> p.getMessageType().equals("ota.recovery.resolved")));
    }

    @Test
    @DisplayName("Inbox 幂等：同业务键同摘要重放不重复推进状态、不重复产结果")
    void replay_sameDigest_idempotent() throws Exception {
        consume(permitMessage("idem-replay", true));
        long outboxAfterFirst = outboxOrder.size();

        // 重放同一条许可消息（同 digest）
        consume(permitMessage("idem-replay", true));
        assertEquals(outboxAfterFirst, outboxOrder.size(), "重放不应重复生产下行结果");

        // VehicleTask 仍只有一个活动 Execution
        assertEquals(1, executionRepository.findActiveByVehicleTaskId(VehicleTaskId.of(vehicleTaskId)).map(e -> 1).orElse(0));
        // 领域状态未被重复推进（attemptNo 仍为 1）
        Execution ex = executionRepository.findActiveByVehicleTaskId(VehicleTaskId.of(vehicleTaskId)).orElseThrow();
        assertEquals(1, ex.getAttemptNo());
    }

    // ==================== 消息构造 ====================

    private ConsumerRecord<String, String> permitMessage(String idem, boolean first) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vehicleTaskId", vehicleTaskId);
        payload.put("idempotencyKey", idem);
        payload.put("installPlanVersion", "PLAN_V1");
        payload.put("packageManifestDigest", "manifest-digest");
        payload.put("conditionSetVersion", "COND_V1");
        return record("ota.execution.permit.requested", "corr-permit", payload, idem);
    }

    private ConsumerRecord<String, String> eventMessage(long seq, String eventId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("executionId", executionId);
        payload.put("eventId", eventId);
        payload.put("sequenceNo", seq);
        payload.put("eventType", "PROGRESS");
        return record("ota.execution.event.reported", "corr-event-" + seq, payload, "evt-" + seq);
    }

    private ConsumerRecord<String, String> resultMessage(String finalStatus, long finalSeq, String idem) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("executionId", executionId);
        payload.put("finalStatus", finalStatus);
        payload.put("finalSequenceNo", finalSeq);
        payload.put("resultDigest", "result-digest");
        return record("ota.execution.result.reported", "corr-result", payload, idem);
    }

    private ConsumerRecord<String, String> recoveryMessage() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("scope", "EXECUTION");
        payload.put("executionId", executionId);
        return record("ota.recovery.requested", "corr-recovery", payload, "rec-1");
    }

    private ConsumerRecord<String, String> record(String messageType, String correlationId,
                                                  ObjectNode payload, String idemKey) throws Exception {
        OtaKafkaEnvelope envelope = OtaKafkaEnvelope.builder()
                .messageId("msg-" + System.nanoTime())
                .messageType(messageType)
                .schemaVersion(1)
                .timestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .deviceId("CGW-001")
                .vin("VIN001")
                .correlationId(correlationId)
                .idempotencyKey(idemKey)
                .payloadDigest("sha256:" + messageType + "-" + idemKey)
                .payload(payload)
                .build();
        return new ConsumerRecord<>("iov.vagw.up.fota", 0, System.nanoTime(), "VIN001",
                objectMapper.writeValueAsString(envelope));
    }

    private void consume(ConsumerRecord<String, String> record) {
        inboundHandler.processMessage(record);
    }

    private static String key(String consumerName, String businessKey) {
        return consumerName + "|" + businessKey;
    }

    // ==================== 辅助 ====================

    private Task buildReleasedTask() {
        Task t = Task.create(TaskId.of(100L), "集成任务", TaskType.NORMAL, ActivityId.of(1L));
        t.setStartTime(startTime);
        t.setEndTime(endTime);
        t.submit();
        t.approve(true, null);
        t.release(Set.of(Vin.of("VIN001")), "IMMEDIATE");
        t.setReleaseTime(now.minusSeconds(120));
        return t;
    }

    private VehicleTask buildReadyVehicleTask() {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(120), startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.grantConsent(false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        return vt;
    }
}
