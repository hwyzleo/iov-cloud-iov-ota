package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.ManifestDigestMetrics;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Mock
    private ManifestDigestMetrics manifestDigestMetrics;

    private InstallPermitService service;

    private final Instant now = Instant.now();
    private final Instant releaseAt = now.minusSeconds(60);
    private final Instant startTime = now.minusSeconds(30);
    private final Instant endTime = now.plusSeconds(3600);
    private final Instant validUntil = now.plusSeconds(1800);

    @BeforeEach
    void setUp() {
        service = new InstallPermitService(permitTokenSigner, manifestDigestMetrics);
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
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now);

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
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now));
    }

    @Test
    @DisplayName("时间窗口外（now>=endTime）-> 抛异常")
    void requestInstall_afterEndTime_throwsException() {
        Task task = buildTask(startTime, now.minusSeconds(1));
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now));
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
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(2L), task, vt, request, null, now));
    }

    @Test
    @DisplayName("授权未通过 -> 抛异常")
    void requestInstall_consentNotGranted_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();
        vt.setConsentState(net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState.PENDING);

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(true)
                .consentPermitted(false)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now));
    }

    @Test
    @DisplayName("包阶段结果未全部成功 -> 抛异常")
    void requestInstall_packageStageNotSucceeded_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(false)
                .validUntil(validUntil)
                .build();

        assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now));
    }

    @Test
    @DisplayName("包清单摘要校验失败 -> 抛异常")
    void requestInstall_manifestDigestMismatch_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of("different-digest"))
                .validUntil(validUntil)
                .build();

        ExecutionStateException e = assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now));
        // 异常必须带实际比较值，否则现场只能看到“校验失败”无法定位
        assertTrue(e.getMessage().contains("different-digest"));
        assertTrue(e.getMessage().contains("digest"));
        // CR-024 §4.28.6：不一致必须计入 mismatch 指标
        verify(manifestDigestMetrics).mismatch();
    }

    @Test
    @DisplayName("包清单摘要大小写/算法前缀差异 -> 判定一致，不抛异常")
    void requestInstall_manifestDigestCaseInsensitive_matches() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of(" SHA-256:DIGEST "))
                .validUntil(validUntil)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now);
        assertEquals(1, execution.getAttemptNo());
    }

    @Test
    @DisplayName("云端未冻结摘要（历史数据为 NULL）-> 跳过校验照常签发许可，不硬阻断")
    void requestInstall_cloudDigestAbsent_skipsVerification() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 1L, "VIN001",
                TaskRevision.initial(), null,
                releaseAt, startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 1L, "scope", now, false);
        assertNull(vt.getSnapshotDigest());

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of("vehicle-side-digest"))
                .validUntil(validUntil)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(1L), task, vt, request, null, now);
        assertEquals(1, execution.getAttemptNo());
        assertEquals(VehicleTaskStatus.EXECUTING, vt.getStatus());
        // CR-024 §4.28.6：跳过校验必须计入「摘要不可校验」，与不一致分开统计
        verify(manifestDigestMetrics).unverifiable();
        verify(manifestDigestMetrics, never()).mismatch();
    }

    @Test
    @DisplayName("CR-024 云端重算值优先：车端值等于重算值即放行（即便冻结列为空）")
    void requestInstall_recomputedDigestMatch_passes() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 1L, "VIN001",
                TaskRevision.initial(), null,
                releaseAt, startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 1L, "scope", now, false);

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of("A1B2C3"))
                .validUntil(validUntil)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(1L), task, vt, request,
                SnapshotDigest.of("a1b2c3"), now);

        assertEquals(1, execution.getAttemptNo());
        // 冻结值取本次重算值，保证“校验事实 == 冻结事实”
        assertEquals("a1b2c3", execution.getPackageManifestDigest().getValue());
    }

    @Test
    @DisplayName("CR-024 车端值与重算值不一致 -> 抛异常并计入 mismatch")
    void requestInstall_recomputedDigestMismatch_throwsException() {
        Task task = buildTask(startTime, endTime);
        VehicleTask vt = buildReadyVehicleTask();

        InstallPermitService.InstallPermitRequest request = InstallPermitService.InstallPermitRequest.builder()
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .expectedPackageManifestDigest(SnapshotDigest.of("car-digest"))
                .validUntil(validUntil)
                .build();

        ExecutionStateException e = assertThrows(ExecutionStateException.class,
                () -> service.requestInstall(ExecutionId.of(1L), task, vt, request,
                        SnapshotDigest.of("cloud-digest"), now));
        assertTrue(e.getMessage().contains("car-digest"));
        assertTrue(e.getMessage().contains("cloud-digest"));
        verify(manifestDigestMetrics).mismatch();
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
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .validUntil(validUntil)
                .build();

        Execution execution = service.requestInstall(ExecutionId.of(2L), task, vt, request, null, now);
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
        vt.applyConsent(ConsentResult.GRANTED, 1L, "scope", now, false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        return vt;
    }
}
