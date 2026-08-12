package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.ccp;

import net.hwyz.iov.cloud.iov.ota.api.contract.FotaV2Request;
import net.hwyz.iov.cloud.iov.ota.api.contract.FotaV2Response;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.RecoveryQueryCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.FotaV2ProtocolService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * CcpFotaV2Controller 测试（CR-012 §6、US-073）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CcpFotaV2Controller v2 端点")
class CcpFotaV2ControllerTest {

    @Mock private FotaV2ProtocolService protocolService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private TaskDetectionAppService taskDetectionAppService;
    @Mock private ConsentAppService consentAppService;
    @Mock private PackageDeliveryAppService packageDeliveryAppService;
    @Mock private ExecutionAppService executionAppService;
    @Mock private ExecutionEventAppService executionEventAppService;
    @Mock private RecoveryAppService recoveryAppService;
    @Mock private LogAppService logAppService;
    @Mock private PolicySyncAppService policySyncAppService;

    private CcpFotaV2Controller controller;

    @BeforeEach
    void setUp() {
        controller = new CcpFotaV2Controller(protocolService, idempotencyService,
                taskDetectionAppService, consentAppService, packageDeliveryAppService,
                executionAppService, executionEventAppService, recoveryAppService,
                logAppService, policySyncAppService);
    }

    @Test
    @DisplayName("detect 端点调用协议校验和检测服务")
    void detect_endpoint_validatesAndDelegates() {
        DetectionCmd cmd = DetectionCmd.builder()
                .vin("VIN001").inventoryMode("DIGEST").inventoryRevision(1L).build();
        FotaV2Request<DetectionCmd> request = buildRequest(cmd, true);

        DetectionResult result = DetectionResult.builder()
                .inventoryDisposition("ACCEPTED")
                .availabilityStatus(AvailabilityStatus.AVAILABLE.getValue())
                .visible(true)
                .matchedTasks(List.of())
                .build();
        when(idempotencyService.execute(any(), any(), any(), any(), any())).thenReturn(result);
        when(taskDetectionAppService.detect(cmd)).thenReturn(result);

        FotaV2Response<DetectionResult> response = controller.detect(request);

        verify(protocolService).validateProtocolVersion("2.0");
        verify(protocolService).validateDeviceBinding("VIN001", "DEV001");
        verify(protocolService).validateReplayProtection(anyLong(), any());
        assertEquals("0", response.getCode());
        assertEquals("ACCEPTED", response.getData().getInventoryDisposition());
    }

    @Test
    @DisplayName("createExecution 写操作要求幂等键")
    void createExecution_writeOperation_requiresIdempotency() {
        ExecutionCreateCmd cmd = ExecutionCreateCmd.builder()
                .vehicleTaskId(1L).idempotencyKey("idem-001").build();
        FotaV2Request<ExecutionCreateCmd> request = buildRequest(cmd, true);

        ExecutionCreateResult result = ExecutionCreateResult.builder()
                .executionId(1L).attemptNo(1).permitToken("token").build();
        when(idempotencyService.execute(any(), any(), any(), any(), any())).thenReturn(result);
        when(executionAppService.requestInstall(cmd)).thenReturn(result);

        FotaV2Response<ExecutionCreateResult> response = controller.createExecution(1L, request);

        // 使用请求信封中的幂等键（buildRequest 设为 idem-key）
        verify(protocolService).validateIdempotencyKey("idem-key", true);
        assertEquals("0", response.getCode());
        assertEquals("token", response.getData().getPermitToken());
    }

    @Test
    @DisplayName("读操作不强制幂等键")
    void recoveryQuery_readOperation_noIdempotencyRequired() {
        FotaV2Request<RecoveryQueryCmd> request = buildRequest(RecoveryQueryCmd.builder().build(), false);
        when(idempotencyService.execute(any(), any(), any(), any(), any())).thenReturn(null);

        controller.recoveryQuery(request);

        // 读操作不强制幂等键：validateIdempotencyKey 仍被调用但校验放行（null, false）
        verify(protocolService).validateIdempotencyKey(isNull(), eq(false));
    }

    private <T> FotaV2Request<T> buildRequest(T data, boolean withIdempotencyKey) {
        return FotaV2Request.<T>builder()
                .protocolVersion("2.0")
                .vin("VIN001")
                .deviceId("DEV001")
                .timestamp(Instant.now().toEpochMilli())
                .nonce("nonce-" + System.nanoTime())
                .idempotencyKey(withIdempotencyKey ? "idem-key" : null)
                .data(data)
                .build();
    }
}
