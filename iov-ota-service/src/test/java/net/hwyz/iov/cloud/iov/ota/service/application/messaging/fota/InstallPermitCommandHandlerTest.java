package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Execution.InstallPermitRequest;
import vehicle.fota.v1.Execution.InstallPermitResponse;
import vehicle.fota.v1.Types.Digest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InstallPermitCommandHandler 映射与拒绝语义测试（CR-014 §5）。
 *
 * <p>重点回归 2026-11-15 实车问题：领域守卫失败必须以 {@code allowed=false + deny_reason}
 * 的 RESPONSE 回答车端，而不是以异常形式上抛引起无限重投与分区阻塞。
 *
 * @author hwyz_leo
 */
@DisplayName("InstallPermitCommandHandler - 请求映射与业务拒绝")
class InstallPermitCommandHandlerTest {

    private final ExecutionAppService appService = mock(ExecutionAppService.class);
    private final InstallPermitCommandHandler handler = new InstallPermitCommandHandler(appService);

    private FotaMessageMetadata metadata(String vehicleTaskId) {
        return new FotaMessageMetadata(
                "req-1", 1000L, "fota-v1", "dev-1", "HWYZTEST900000001",
                vehicleTaskId, null, "idem-1", "vehicle.fota.v1.InstallPermitRequest",
                "msg-1", null, MessageKind.MESSAGE_KIND_REQUEST, null, null);
    }

    private InstallPermitRequest request() {
        return InstallPermitRequest.newBuilder()
                .setTaskRevision(3L)
                .setInstallPlanVersion("PLAN_V1")
                .setPackageManifestDigest(Digest.newBuilder()
                        .setAlgorithm("sha-256").setValueHex("a1b2c3").build())
                .setConditionSetVersion("COND_V1")
                .build();
    }

    @Test
    @DisplayName("守卫通过 -> allowed=true，映射 install_plan_version/摘要到命令")
    void handle_success_mapsRequestAndReturnsPermit() {
        when(appService.requestInstall(any())).thenReturn(ExecutionCreateResult.builder()
                .executionId(1001L).attemptNo(1).permitToken("token-1")
                .validUntil(Instant.ofEpochMilli(2000L)).installPlanVersion("PLAN_V1").build());

        InstallPermitResponse resp = handler.handle(metadata("3"), request());

        assertEquals("0", resp.getStatus().getCode());
        assertTrue(resp.getAllowed());
        assertEquals("1001", resp.getExecutionId());
        assertEquals("permit-1001", resp.getPermitId());
        assertEquals("token-1", resp.getPermitToken());
        assertEquals(1, resp.getAttemptNo());
        assertEquals("PLAN_V1", resp.getInstallPlanVersion());
        assertEquals(2000L, resp.getValidUntilMs());

        ArgumentCaptor<ExecutionCreateCmd> captor = ArgumentCaptor.forClass(ExecutionCreateCmd.class);
        verify(appService).requestInstall(captor.capture());
        ExecutionCreateCmd cmd = captor.getValue();
        assertEquals("HWYZTEST900000001", cmd.getVin());
        assertEquals(3L, cmd.getVehicleTaskId());
        assertEquals("PLAN_V1", cmd.getInstallPlanVersion());
        assertEquals("a1b2c3", cmd.getPackageManifestDigest());
        assertEquals("a1b2c3", cmd.getExpectedPackageManifestDigest());
        assertEquals("COND_V1", cmd.getConditionSetVersion());
    }

    @Test
    @DisplayName("领域守卫失败（如摘要校验失败）-> allowed=false + deny_reason，不抛异常")
    void handle_domainGuardFailure_returnsDeny() {
        when(appService.requestInstall(any())).thenThrow(new ExecutionStateException(
                "包清单摘要校验失败：车端[a1b2c3] 云端[d4e5f6]"));

        InstallPermitResponse resp = handler.handle(metadata("3"), request());

        assertFalse(resp.getAllowed());
        assertEquals("PERMIT_DENIED", resp.getStatus().getCode());
        assertFalse(resp.getStatus().getMessage().isBlank());
        assertTrue(resp.hasDenyReason());
        assertTrue(resp.getDenyReason().contains("包清单摘要校验失败"));
        assertTrue(resp.getNextRetryAtMs() > 0);
        assertFalse(resp.hasExecutionId(), "拒绝时不得下发 execution_id/permit");
        assertFalse(resp.hasPermitToken());
    }

    @Test
    @DisplayName("缺少 vehicle_task_id -> INVALID_REQUEST 拒绝，不触碰应用服务")
    void handle_missingVehicleTaskId_returnsInvalidRequest() {
        InstallPermitResponse resp = handler.handle(metadata(null), request());

        assertFalse(resp.getAllowed());
        assertEquals("INVALID_REQUEST", resp.getStatus().getCode());
        assertTrue(resp.getDenyReason().contains("vehicle_task_id"));
        verifyNoInteractions(appService);
    }

    @Test
    @DisplayName("vehicle_task_id 非数字 -> INVALID_REQUEST 拒绝")
    void handle_nonNumericVehicleTaskId_returnsInvalidRequest() {
        InstallPermitResponse resp = handler.handle(metadata("abc"), request());

        assertFalse(resp.getAllowed());
        assertEquals("INVALID_REQUEST", resp.getStatus().getCode());
        verifyNoInteractions(appService);
    }
}
