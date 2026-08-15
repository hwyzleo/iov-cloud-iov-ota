package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.*;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * FOTA PayloadType Router 单元测试（CR-014 §5、验收 3）
 *
 * <p>registry 驱动解析；未知/未注册类型 fail-closed；INBOUND 全覆盖约束。
 *
 * @author hwyz_leo
 */
@DisplayName("FotaPayloadRouter - registry 驱动解析")
class FotaPayloadRouterTest {

    private FotaPayloadRouter router;

    @BeforeEach
    void setUp() {
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(new ObjectMapper());
        guard.verify();
        FotaEnvelopeFactory factory = new FotaEnvelopeFactory(guard);
        FotaOutboxAppender outboxAppender = mock(FotaOutboxAppender.class);
        KafkaMessagingMetricsService metrics = mock(KafkaMessagingMetricsService.class);

        List<FotaPayloadHandler<?>> handlers = List.of(
                new TaskCheckPayloadHandler(mock(TaskCheckCommandHandler.class), factory, outboxAppender, metrics),
                new ConsentPayloadHandler(mock(ConsentCommandHandler.class), factory, outboxAppender, metrics),
                new DownloadGrantPayloadHandler(mock(DownloadGrantCommandHandler.class), factory, outboxAppender, metrics),
                new StageResultPayloadHandler(mock(StageResultCommandHandler.class), factory, outboxAppender, metrics),
                new InstallPermitPayloadHandler(mock(InstallPermitCommandHandler.class), factory, outboxAppender, metrics),
                new ExecutionEventPayloadHandler(mock(ExecutionEventCommandHandler.class), factory, outboxAppender, metrics),
                new ControlAckPayloadHandler(mock(ControlAckCommandHandler.class), factory, outboxAppender, metrics),
                new FinalResultPayloadHandler(mock(FinalResultCommandHandler.class), factory, outboxAppender, metrics),
                new LogGrantPayloadHandler(mock(LogGrantCommandHandler.class), factory, outboxAppender, metrics),
                new LogResultPayloadHandler(mock(LogResultCommandHandler.class), factory, outboxAppender, metrics),
                new ReconcilePayloadHandler(mock(ReconcileCommandHandler.class), factory, outboxAppender, metrics),
                new PolicyPayloadHandler(mock(PolicyCommandHandler.class), factory, outboxAppender, metrics));

        router = new FotaPayloadRouter(guard, handlers);
        router.init();
    }

    @Test
    @DisplayName("已注册 INBOUND payload_type 解析到对应 handler")
    void resolve_registered_types() {
        assertEquals("vehicle.fota.v1.TaskCheckRequest",
                router.resolve("vehicle.fota.v1.TaskCheckRequest").payloadType());
        assertEquals("vehicle.fota.v1.ExecutionEvent",
                router.resolve("vehicle.fota.v1.ExecutionEvent").payloadType());
        assertEquals("vehicle.fota.v1.PolicyRequest",
                router.resolve("vehicle.fota.v1.PolicyRequest").payloadType());
    }

    @Test
    @DisplayName("未知 / 未注册 payload_type → fail-closed")
    void resolve_unknown_fails_closed() {
        assertThrows(OtaKafkaMessagingException.class, () -> router.resolve("vehicle.ota.v1.LegacyRequest"));
        assertThrows(OtaKafkaMessagingException.class, () -> router.resolve("ota.task.detect.requested"));
    }
}
