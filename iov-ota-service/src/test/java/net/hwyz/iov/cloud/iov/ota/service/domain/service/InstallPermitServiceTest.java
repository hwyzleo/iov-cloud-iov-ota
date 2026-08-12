package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PermitTokenSigner;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * InstallPermitService 安装许可测试（CR-012 §5.5）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InstallPermitService 安装许可")
class InstallPermitServiceTest {

    @Mock
    private PermitTokenSigner permitTokenSigner;

    private InstallPermitService service;

    private final Instant now = Instant.now();
    private final Instant releaseAt = now.minusSeconds(60);
    private final Instant startTime = now.minusSeconds(30);
    private final Instant endTime = now.plusSeconds(3600);
    private final Instant validUntil = now.plusSeconds(1800);

    @BeforeEach
    void setUp() {
        service = new InstallPermitService(permitTokenSigner);
        when(permitTokenSigner.sign(any(ExecutionId.class), any(VehicleTaskId.class), anyInt(), any(Instant.class)))
                .thenReturn(PermitToken.of("signed-token", validUntil));
    }

    @Test
    @DisplayName("满足全部守卫 -> 创建 Execution 并绑定到 VehicleTask")
    void requestInstall_allGuardsPassed_createsExecution() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .installPlanVersion("PLAN_V1")
                .packageManifestDigest(SnapshotDigest.of("digest"))
                .conditionSetVersion("COND_V1")
                .validUntil(validUntil)
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(1L), task, vt, request, now);

        assertEquals(1, execution.getAttemptNo());
        assertEquals("signed-token", execution.getPermitToken().getToken());
        assertEquals(VehicleTaskStatus.EXECUTING, vt.getStatus());
        assertEquals(1, vt.getLastAttemptNo());
        assertTrue(vt.hasActiveExecution());
    }

    @Test
    @DisplayName("时间窗口外（now<startTime）-> 抛异常")
    void requestInstall_beforeStartTime_throwsException() {
        Task task = buildTask(now.plusSeconds(60), endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, now));
    }

    @Test
    @DisplayName("时间窗口外（now>=endTime）-> 抛异常")
    void requestInstall_afterEndTime_throwsException() {
        Task task = buildTask(startTime, now.minusSeconds(1));
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, now));
    }

    @Test
    @DisplayName("已有活动执行 -> 抛异常（活动 Execution 唯一性）")
    void requestInstall_activeExecutionExists_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();
        // 先绑定一个执行
        vt.attachExecution(ExecutionId.of(99L), 1);

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(2L), task, vt, request, now));
    }

    @Test
    @DisplayName("授权未通过 -> 抛异常")
    void requestInstall_consentNotGranted_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();
        vt.setConsentState(net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState.PENDING);

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(true)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, now));
    }

    @Test
    @DisplayName("包阶段结果未全部成功 -> 抛异常")
    void requestInstall_packageStageNotSucceeded_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(false)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, now));
    }

    @Test
    @DisplayName("包清单摘要校验失败 -> 抛异常")
    void requestInstall_manifestDigestMismatch_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of("different-digest"))
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, now));
    }

    @Test
    @DisplayName("attemptNo 递增：第二次安装 attemptNo=2")
    void requestInstall_attemptNoIncrements() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();
        // 模拟之前有过一次尝试
        vt.setLastAttemptNo(1);

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(2L), task, vt, request, now);
        assertEquals(2, execution.getAttemptNo());
        assertEquals(2, vt.getLastAttemptNo());
    }

    private Task buildTask(Instant startTime, Instant endTime) {
        Task task = Task.create(TaskId.of(1L), "测试任务", TaskType.NORMAL, ActivityId.of(100L));
        task.submit();
        task.approve(true, null);
        task.setStartTime(startTime);
        // 发布校验要求 endTime 在未来，先设未来值发布，再覆盖为测试值
        task.setEndTime(now.plusSeconds(7200));
        task.release(Set.of(Vin.of("VIN001")), "IMMEDIATE");
        task.setReleaseTime(releaseAt);
        task.setEndTime(endTime);
        return task;
    }

    private VehicleTask buildReadyVehicleTask() {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 1L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                releaseAt, startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.grantConsent(false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        return vt;
    }
}
