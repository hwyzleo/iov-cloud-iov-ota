package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionFinalizeResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ConsentPolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InstallPermitService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuSoftwareUnitMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 最终结果收口与迟到数据守卫测试（IOV-OTA-DSN-CR-025 §4.29.8）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExecutionAppService 收口与迟到数据")
class ExecutionAppServiceFinalizeTest {

    @Mock private VehicleTaskRepository vehicleTaskRepository;
    @Mock private ExecutionRepository executionRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private InstallPermitService installPermitService;
    @Mock private ConsentPolicy consentPolicy;
    @Mock private VehicleTaskConsentRepository vehicleTaskConsentRepository;
    @Mock private OutboxRepository outboxRepository;
    @Mock private ExecutionEcuResultMapper executionEcuResultMapper;
    @Mock private ExecutionEcuSoftwareUnitMapper executionEcuSoftwareUnitMapper;
    @Mock private VehicleTaskPackageService vehicleTaskPackageService;
    @Mock private PackageManifestDigestService packageManifestDigestService;
    @Mock private RetryTrajectoryService retryTrajectoryService;

    @InjectMocks
    private ExecutionAppService service;

    private Execution execution() {
        return Execution.permit(
                ExecutionId.of(5001L), VehicleTaskId.of(9001L), 1,
                TaskRevision.initial(), "PLAN_V1", SnapshotDigest.of("digest"), "COND_V1",
                PermitToken.of("token", Instant.now().plusSeconds(600)),
                Instant.now().plusSeconds(600));
    }

    @Test
    @DisplayName("终态执行拒绝迟到最终结果，不推进车辆任务")
    void finalize_terminalExecution_rejected() {
        Execution execution = execution();
        execution.cancel();
        when(executionRepository.getById(ExecutionId.of(5001L))).thenReturn(Optional.of(execution));

        ExecutionFinalizeResult result = service.finalizeExecution(ExecutionFinalizeCmd.builder()
                .executionId(5001L).finalStatus(ExecutionStatus.SUCCEEDED.getValue()).finalSequenceNo(3L).build());

        assertFalse(result.isResultAccepted());
        assertEquals("LATE_FINAL_RESULT", result.getReason());
        assertEquals(ExecutionStatus.CANCELED.getValue(), result.getExecutionStatus());
        verify(executionRepository, never()).save(any());
        verify(vehicleTaskRepository, never()).save(any());
        verify(outboxRepository, never()).append(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("CR-026：收口写入车端起止时间与云端受理时间")
    void finalize_writesExecutionTiming() {
        Execution execution = execution();
        execution.defineFinalSequenceNo(1L);
        execution.receiveEvent(1L);
        when(executionRepository.getById(ExecutionId.of(5001L))).thenReturn(Optional.of(execution));
        Instant startedAt = Instant.ofEpochMilli(1_800_000_000_000L);
        Instant completedAt = Instant.ofEpochMilli(1_800_000_120_000L);

        ExecutionFinalizeResult result = service.finalizeExecution(ExecutionFinalizeCmd.builder()
                .executionId(5001L).finalStatus(ExecutionStatus.SUCCEEDED.getValue())
                .finalSequenceNo(1L).startedAt(startedAt).completedAt(completedAt).build());

        assertTrue(result.isResultAccepted());
        assertEquals(startedAt, execution.getStartTime());
        assertEquals(completedAt, execution.getEndTime());
        assertNotNull(execution.getStartReceivedTime());
        assertNotNull(execution.getEndReceivedTime());
        assertEquals(120_000L, execution.durationMs());
    }

    @Test
    @DisplayName("车辆任务已被取代时收口不推进其状态（不抛 VehicleTaskStateException）")
    void finalize_supersededVehicleTask_skipsAdvance() {
        Execution execution = execution();
        execution.defineFinalSequenceNo(1L);
        execution.receiveEvent(1L);
        when(executionRepository.getById(ExecutionId.of(5001L))).thenReturn(Optional.of(execution));

        VehicleTask superseded = VehicleTask.reconstitute(
                VehicleTaskId.of(9001L), 100L, "VIN001", TaskRevision.initial(), SnapshotDigest.of("digest"),
                VehicleTaskStatus.SUPERSEDED, DownloadReadyState.VERIFIED, ConsentState.NOT_REQUIRED,
                Instant.now(), Instant.now(), Instant.now().plusSeconds(600),
                VehicleTaskId.of(9002L), "ABANDON", "KEEP", null, 1, null,
                false, null, null, null, null, null, null, 5L);
        when(vehicleTaskRepository.getById(VehicleTaskId.of(9001L))).thenReturn(Optional.of(superseded));

        ExecutionFinalizeResult result = service.finalizeExecution(ExecutionFinalizeCmd.builder()
                .executionId(5001L).finalStatus(ExecutionStatus.FAILED.getValue()).finalSequenceNo(1L).build());

        assertTrue(result.isResultAccepted());
        assertEquals(VehicleTaskStatus.SUPERSEDED.getValue(), result.getVehicleTaskStatus(),
                "被取代车辆任务状态不被最终结果推进");
        assertEquals(VehicleTaskStatus.SUPERSEDED, superseded.getStatus());
        verify(vehicleTaskRepository).save(superseded);
    }
}
