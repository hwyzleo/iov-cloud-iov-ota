package net.hwyz.iov.cloud.iov.ota.service.integration;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PackageManifestDigestService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleTaskPackageService;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InstallPermitService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.ManifestDigestMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskSnapshotMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository.TaskVehicleRepositoryImpl;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.LocalPermitTokenSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CR-024 集成测试：发布期冻结包清单摘要 → 许可期云端重算 → 校验/拒绝（§4.28.2～§4.28.6）。
 *
 * <p>链路：真实 {@link VehicleTaskPackageService}（mock 投影数据源）+ 真实
 * {@link PackageManifestDigestService} + 真实 {@link TaskVehicleRepositoryImpl}（mock DAO）
 * + 真实 {@link InstallPermitService} 与指标实现；只替换持久化与外部数据源。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("CR-024 集成测试 - 包清单摘要冻结与校验")
class ManifestDigestFlowIntegrationTest {

    private ActivityUpgradeTargetMapper activityUpgradeTargetMapper;
    private SoftwareBuildVersionMapper softwareBuildVersionMapper;
    private SoftwareBuildVersionPackageMapper softwareBuildVersionPackageMapper;
    private SoftwarePackageMapper softwarePackageMapper;
    private VehicleTaskPackageMapper vehicleTaskPackageMapper;
    private TaskVehicleMapper taskVehicleMapper;
    private VehicleTaskSnapshotMapper vehicleTaskSnapshotMapper;

    private VehicleTaskPackageService vehicleTaskPackageService;
    private PackageManifestDigestService digestService;
    private ManifestDigestMetricsService metricsService;
    private TaskVehicleRepositoryImpl taskVehicleRepository;
    private InstallPermitService installPermitService;

    private final Instant now = Instant.now();
    private final Instant startTime = now.minusSeconds(60);
    private final Instant endTime = now.plusSeconds(3600);
    private final Instant releaseAt = now.minusSeconds(120);

    @BeforeEach
    void setUp() {
        activityUpgradeTargetMapper = mock(ActivityUpgradeTargetMapper.class);
        softwareBuildVersionMapper = mock(SoftwareBuildVersionMapper.class);
        softwareBuildVersionPackageMapper = mock(SoftwareBuildVersionPackageMapper.class);
        softwarePackageMapper = mock(SoftwarePackageMapper.class);
        vehicleTaskPackageMapper = mock(VehicleTaskPackageMapper.class);
        taskVehicleMapper = mock(TaskVehicleMapper.class);
        vehicleTaskSnapshotMapper = mock(VehicleTaskSnapshotMapper.class);

        vehicleTaskPackageService = new VehicleTaskPackageService(null, activityUpgradeTargetMapper,
                vehicleTaskPackageMapper, softwareBuildVersionMapper, softwareBuildVersionPackageMapper,
                softwarePackageMapper);
        digestService = new PackageManifestDigestService();
        metricsService = new ManifestDigestMetricsService();
        taskVehicleRepository = new TaskVehicleRepositoryImpl(taskVehicleMapper, vehicleTaskSnapshotMapper);
        installPermitService = new InstallPermitService(new LocalPermitTokenSigner(), metricsService);

        // 投影数据源：活动升级对象 → 软件内部版本 → 软件包
        ActivityUpgradeTargetPo target = new ActivityUpgradeTargetPo();
        target.setSoftwareBuildVersionId(5001L);
        target.setVehicleNodeCode("CCU");
        target.setSoftwareTargetCode("CCU_FOTA");
        when(activityUpgradeTargetMapper.selectPoByActivityId(100L)).thenReturn(List.of(target));

        SoftwareBuildVersionPo build = new SoftwareBuildVersionPo();
        build.setSoftwareBuildVer("V2.0.0");
        when(softwareBuildVersionMapper.selectPoById(5001L)).thenReturn(build);

        SoftwareBuildVersionPackagePo link = new SoftwareBuildVersionPackagePo();
        link.setSoftwarePackageId(7001L);
        link.setSoftwareTargetCode("CCU_FOTA");
        when(softwareBuildVersionPackageMapper.selectPoBySoftwareBuildVersionId(5001L)).thenReturn(List.of(link));

        SoftwarePackagePo pkg = new SoftwarePackagePo();
        pkg.setId(7001L);
        pkg.setPackageCode("PKG-CCU-001");
        pkg.setPackageSize(2048L);
        pkg.setPackageSha256("aa11bb22");
        when(softwarePackageMapper.selectPoById(7001L)).thenReturn(pkg);

        when(vehicleTaskPackageMapper.selectByVehicleTaskId(anyLong())).thenReturn(List.of());
    }

    private Task releasedTask() {
        Task task = Task.create(TaskId.of(9001L), "摘要冻结任务", TaskType.NORMAL, ActivityId.of(100L));
        task.setPhase(TaskPhase.VALIDATION);
        task.setState(TaskState.RELEASED);
        task.setReleaseTime(releaseAt);
        task.setActualReleaseTime(releaseAt);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        return task;
    }

    private VehicleTask readyVehicleTask(SnapshotDigest digest) {
        VehicleTask vt = VehicleTask.create(VehicleTaskId.of(10L), 9001L, "VIN001",
                TaskRevision.initial(), digest, releaseAt, startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 1L, "scope", now, false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        return vt;
    }

    private InstallPermitService.InstallPermitRequest request(String expectedDigest) {
        return InstallPermitService.InstallPermitRequest.builder()
                .installPlanVersion("task-9001-r1")
                .expectedPackageManifestDigest(SnapshotDigest.of(expectedDigest))
                .validUntil(now.plusSeconds(1800))
                .consentRequired(false)
                .consentPermitted(true)
                .allPackageStageResultsSucceeded(true)
                .build();
    }

    @Test
    @DisplayName("发布冻结 → 许可期重算：同值放行，且冻结值等于重算值")
    void freezeThenPermit_sameDigest_allowed() {
        Task task = releasedTask();

        // 1. 发布期冻结（TaskEventHandler 的行为）
        VehicleTaskSnapshotSeal seal = digestService.seal(vehicleTaskPackageService.projectForTask(task),
                "task-9001-r1", 9001L);
        when(taskVehicleMapper.selectByTaskIdAndVin(anyLong(), anyString())).thenReturn(null);
        when(taskVehicleMapper.batchInsertPo(anyList())).thenReturn(1);
        when(taskVehicleMapper.selectIdVinByTaskId(9001L))
                .thenReturn(List.of(TaskVehiclePo.builder().id(10L).taskId(9001L).vin("VIN001").build()));
        taskVehicleRepository.batchCreate(task, Set.of(Vin.of("VIN001")), seal);

        ArgumentCaptor<List<TaskVehiclePo>> inserted = ArgumentCaptor.forClass(List.class);
        verify(taskVehicleMapper).batchInsertPo(inserted.capture());
        assertEquals(seal.getSnapshotDigest(), inserted.getValue().get(0).getSnapshotDigest());

        // 2. 许可期云端重算（ExecutionAppService 的行为）
        VehicleTask vt = readyVehicleTask(SnapshotDigest.of(seal.getSnapshotDigest()));
        List<DetectionResult.MatchedPackage> projected =
                vehicleTaskPackageService.project(vt, task, null);
        SnapshotDigest recomputed = SnapshotDigest.of(
                digestService.digest(projected, "task-9001-r1", 9001L));

        // 冻结值与重算值必须同源同值（否则会退化成永久拒绝）
        assertEquals(seal.getSnapshotDigest(), recomputed.getValue());

        // 3. 车端回传同值 → 签发许可
        Execution execution = installPermitService.requestInstall(ExecutionId.of(1L), task, vt,
                request(seal.getSnapshotDigest()), recomputed, now);
        assertEquals(1, execution.getAttemptNo());
        assertEquals(seal.getSnapshotDigest(), execution.getPackageManifestDigest().getValue());
        assertEquals(0L, metricsService.count(ManifestDigestMetricsService.UNVERIFIABLE));
        assertEquals(0L, metricsService.count(ManifestDigestMetricsService.MISMATCH));
    }

    @Test
    @DisplayName("车端摘要被篡改 → PERMIT_DENIED 且计入 mismatch，不签发许可")
    void tamperedDigest_denied() {
        Task task = releasedTask();
        VehicleTaskSnapshotSeal seal = digestService.seal(vehicleTaskPackageService.projectForTask(task),
                "task-9001-r1", 9001L);
        VehicleTask vt = readyVehicleTask(SnapshotDigest.of(seal.getSnapshotDigest()));
        SnapshotDigest recomputed = SnapshotDigest.of(digestService.digest(
                vehicleTaskPackageService.project(vt, task, null), "task-9001-r1", 9001L));

        ExecutionStateException e = assertThrows(ExecutionStateException.class,
                () -> installPermitService.requestInstall(ExecutionId.of(1L), task, vt,
                        request("deadbeef"), recomputed, now));

        assertTrue(e.getMessage().contains("deadbeef"));
        assertEquals(1L, metricsService.count(ManifestDigestMetricsService.MISMATCH));
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        assertFalse(vt.hasActiveExecution());
    }

    @Test
    @DisplayName("存量任务未冻结摘要 → fail-open 放行并计入 unverifiable")
    void legacyTaskWithoutDigest_failOpen() {
        Task task = releasedTask();
        VehicleTask vt = readyVehicleTask(null);
        assertNull(vt.getSnapshotDigest());

        Execution execution = installPermitService.requestInstall(ExecutionId.of(1L), task, vt,
                request("vehicle-side-digest"), SnapshotDigest.of("cloud-recomputed"), now);

        assertEquals(1, execution.getAttemptNo());
        assertEquals(1L, metricsService.count(ManifestDigestMetricsService.UNVERIFIABLE));
        assertEquals(0L, metricsService.count(ManifestDigestMetricsService.MISMATCH));
    }
}
