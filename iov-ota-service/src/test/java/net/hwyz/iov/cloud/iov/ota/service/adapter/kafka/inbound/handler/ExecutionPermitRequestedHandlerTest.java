package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CR-013 安装许可处理器单元测试：ota.execution.permit.requested → ota.execution.permitted
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExecutionPermitRequestedHandler - 安装许可申请")
class ExecutionPermitRequestedHandlerTest {

    @Mock private ExecutionAppService executionAppService;
    @Mock private KafkaOutboxRepository outboxRepository;

    private ExecutionPermitRequestedHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        handler = new ExecutionPermitRequestedHandler(objectMapper, outboxRepository,
                new KafkaMessagingMetricsService(), executionAppService);
        when(outboxRepository.append(any())).thenReturn(99L);
        when(executionAppService.requestInstall(any())).thenReturn(ExecutionCreateResult.builder()
                .executionId(1001L)
                .attemptNo(1)
                .permitToken("permit-token-1")
                .validUntil(Instant.now().plusSeconds(1800))
                .taskRevision(1L)
                .installPlanVersion("PLAN_V1")
                .build());
    }

    private OtaKafkaEnvelope envelope(String idempotencyKey) {
        return OtaKafkaEnvelope.builder()
                .messageId("msg-1").messageType(OtaMessageType.EXECUTION_PERMIT_REQUESTED.getValue())
                .schemaVersion(1).vin("VIN001").correlationId("corr-1").build();
    }

    private JsonNode permitPayload(String idempotencyKey) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vehicleTaskId", 10L);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("installPlanVersion", "PLAN_V1");
        return payload;
    }

    @Test
    @DisplayName("申请安装成功，生产 ota.execution.permitted 并携带许可结果")
    void handle_appendPermittedResult() {
        OtaKafkaEnvelope envelope = envelope("idem-001");
        Long outboxId = handler.handle(envelope, permitPayload("idem-001"));

        assertEquals(99L, outboxId);
        ArgumentCaptor<KafkaOutboxPo> captor = ArgumentCaptor.forClass(KafkaOutboxPo.class);
        verify(outboxRepository).append(captor.capture());
        KafkaOutboxPo po = captor.getValue();
        assertEquals(OtaMessageType.EXECUTION_PERMITTED.getValue(), po.getMessageType());
        assertEquals("VIN001", po.getMessageKey());
        assertEquals("corr-1", po.getCorrelationId());
        assertTrue(po.getPayloadJson().contains("\"executionId\":1001"));
        assertTrue(po.getPayloadJson().contains("permitToken"));
    }

    @Test
    @DisplayName("businessKey 优先使用 idempotencyKey")
    void businessKey_usesIdempotencyKey() {
        assertEquals("VIN001:idem-001",
                handler.businessKey(envelope("idem-001"), permitPayload("idem-001")));
    }

    @Test
    @DisplayName("businessKey 缺 idempotencyKey 时回退到 vin+vehicleTaskId")
    void businessKey_fallbacksToVehicleTask() {
        assertEquals("VIN001:10",
                handler.businessKey(envelope(null), permitPayload(null)));
    }

    @Test
    @DisplayName("摘要冲突生产 ota.execution.permit-denied 拒绝事件")
    void handleConflict_appendPermitDenied() {
        OtaKafkaEnvelope envelope = envelope("idem-001");
        Long outboxId = handler.handleConflict(envelope, permitPayload("idem-001"), "幂等冲突");

        assertEquals(99L, outboxId);
        ArgumentCaptor<KafkaOutboxPo> captor = ArgumentCaptor.forClass(KafkaOutboxPo.class);
        verify(outboxRepository).append(captor.capture());
        assertEquals(OtaMessageType.EXECUTION_PERMIT_DENIED.getValue(), captor.getValue().getMessageType());
        assertTrue(captor.getValue().getPayloadJson().contains("rejected"));
    }
}
