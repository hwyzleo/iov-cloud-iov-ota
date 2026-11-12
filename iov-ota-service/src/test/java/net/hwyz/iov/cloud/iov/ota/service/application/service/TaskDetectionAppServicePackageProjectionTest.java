package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskAvailabilityService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 任务检测包清单投影单测。
 *
 * <p>回归：TaskCheckResponse.task.package_list 曾经恒为空（TaskDetectionAppService 不读取
 * 活动升级对象/软件内部版本/软件包），导致车端 doDownload() 空循环、FOTA 业务静默卡死。</p>
 */
@DisplayName("TaskDetectionAppService - 任务包清单投影")
class TaskDetectionAppServicePackageProjectionTest {

    private final VehicleTaskRepository vehicleTaskRepository = mock(VehicleTaskRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final VehicleInventoryApplicationService inventoryAppService =
            mock(VehicleInventoryApplicationService.class);
    private final TaskAvailabilityService availabilityService = mock(TaskAvailabilityService.class);
    private final VehicleInventoryMapper inventoryMapper = mock(VehicleInventoryMapper.class);
    private final VehicleInventoryItemMapper inventoryItemMapper = mock(VehicleInventoryItemMapper.class);
    private final VehicleInventorySoftwareUnitMapper inventorySoftwareUnitMapper =
            mock(VehicleInventorySoftwareUnitMapper.class);
    private final ActivityUpgradeTargetMapper upgradeTargetMapper = mock(ActivityUpgradeTargetMapper.class);
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper = mock(VehicleTaskPackageMapper.class);
    private final SoftwareBuildVersionMapper buildVersionMapper = mock(SoftwareBuildVersionMapper.class);
    private final SoftwareBuildVersionPackageMapper buildVersionPackageMapper =
            mock(SoftwareBuildVersionPackageMapper.class);
    private final SoftwarePackageMapper softwarePackageMapper = mock(SoftwarePackageMapper.class);

    private TaskDetectionAppService service;

    @BeforeEach
    void setUp() {
        // 包清单投影已提取到 VehicleTaskPackageService（与下载完成判定共用同一事实）
        VehicleTaskPackageService packageService = new VehicleTaskPackageService(
                taskRepository, upgradeTargetMapper, vehicleTaskPackageMapper,
                buildVersionMapper, buildVersionPackageMapper, softwarePackageMapper);
        service = new TaskDetectionAppService(vehicleTaskRepository, taskRepository, inventoryAppService,
                availabilityService, inventoryMapper, inventoryItemMapper, inventorySoftwareUnitMapper,
                upgradeTargetMapper, packageService);
    }

    @Test
    @DisplayName("ACCEPTED + 活动升级对象 → 任务快照携带 activityId/installPlanVersion/package_list")
    void projects_package_list_from_activity_targets() {
        VehicleTask vt = VehicleTask.create(VehicleTaskId.of(1001L), 2002L, "HWYZTEST900000001",
                TaskRevision.of(3L), SnapshotDigest.of("digest-3"),
                Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"));
        when(vehicleTaskRepository.findVisibleByVin(anyString())).thenReturn(List.of(vt));

        Task task = Task.create(TaskId.of(2002L), "demo", TaskType.values()[0], ActivityId.of(3003L));
        when(taskRepository.getById(any())).thenReturn(Optional.of(task));

        when(inventoryAppService.handleInventory(any())).thenReturn(
                VehicleInventoryApplicationService.InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.ACCEPTED)
                        .inventoryModel("MULTI_TARGET")
                        .canonicalizationVersion(2)
                        .inventoryRevision(23L)
                        .build());
        when(availabilityService.evaluate(any(), any(), any())).thenReturn(
                new TaskAvailabilityService.AvailabilityResult(AvailabilityStatus.AVAILABLE, true, true, true));

        // Canonical Inventory：CCU_GEN1 / ECU_IMAGE 当前版本 V1.0.0
        VehicleInventoryPo header = VehicleInventoryPo.builder().id(9L).vin("HWYZTEST900000001")
                .inventoryRevision(23L).canonicalizationVersion(2).build();
        when(inventoryMapper.selectLatestByVin(anyString())).thenReturn(header);
        VehicleInventoryItemPo item = VehicleInventoryItemPo.builder().id(11L).ecuId("CCU_GEN1").build();
        when(inventoryItemMapper.selectByInventoryId(9L)).thenReturn(List.of(item));
        VehicleInventorySoftwareUnitPo unit = VehicleInventorySoftwareUnitPo.builder()
                .id(21L).softwareTargetCode("ECU_IMAGE").softwarePartNumber("SPN-CCU")
                .swVersion("V1.0.0").build();
        when(inventorySoftwareUnitMapper.selectByInventoryItemId(11L)).thenReturn(List.of(unit));

        // 活动升级对象 → 软件内部版本 → 软件包
        when(upgradeTargetMapper.selectPoByActivityId(3003L)).thenReturn(List.of(
                ActivityUpgradeTargetPo.builder().id(1L).activityId(3003L)
                        .vehicleNodeCode("CCU_GEN1").softwareTargetCode("ECU_IMAGE")
                        .partCode("SPN-CCU").softwareBuildVersionId(55L).build()));
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(1001L)).thenReturn(List.of());
        when(buildVersionMapper.selectPoById(55L)).thenReturn(
                SoftwareBuildVersionPo.builder().id(55L).deviceCode("CCU").softwarePn("SPN-CCU")
                        .softwareBuildVer("V2.0.0").build());
        when(buildVersionPackageMapper.selectPoBySoftwareBuildVersionId(55L)).thenReturn(List.of(
                SoftwareBuildVersionPackagePo.builder().softwareBuildVersionId(55L)
                        .softwarePackageId(77L).softwareTargetCode("ECU_IMAGE").build()));
        when(softwarePackageMapper.selectPoById(77L)).thenReturn(
                SoftwarePackagePo.builder().id(77L).packageCode("PKG-CCU-1")
                        .packageSize(2048L).packageSha256("sha256hex").build());

        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin("HWYZTEST900000001");
        cmd.setInventoryMode("FULL");

        DetectionResult result = service.detect(cmd);

        assertEquals("ACCEPTED", result.getInventoryDisposition());
        assertEquals(1, result.getMatchedTasks().size());
        DetectionResult.MatchedVehicleTask matched = result.getMatchedTasks().get(0);
        assertEquals(3003L, matched.getActivityId());
        assertEquals("task-2002-r3", matched.getInstallPlanVersion());
        assertEquals(1, matched.getPackages().size());
        DetectionResult.MatchedPackage pkg = matched.getPackages().get(0);
        assertEquals("PKG-CCU-1", pkg.getPackageId());
        assertEquals("V2.0.0", pkg.getPackageRevision());
        assertEquals("CCU_GEN1", pkg.getEcuId());
        assertEquals("ECU_IMAGE", pkg.getComponentId());
        assertEquals("V1.0.0", pkg.getSourceVersion());
        assertEquals("V2.0.0", pkg.getTargetVersion());
        assertEquals(2048L, pkg.getSizeBytes());
        assertEquals("sha-256", pkg.getDigestAlgorithm());
        assertEquals("sha256hex", pkg.getDigestHex());
    }

    @Test
    @DisplayName("活动无升级对象/无包 → package_list 为空（由 handler 拒绝放行）")
    void empty_activity_yields_empty_package_list() {
        VehicleTask vt = VehicleTask.create(VehicleTaskId.of(1001L), 2002L, "HWYZTEST900000001",
                TaskRevision.of(1L), null,
                Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"));
        when(vehicleTaskRepository.findVisibleByVin(anyString())).thenReturn(List.of(vt));
        when(taskRepository.getById(any())).thenReturn(
                Optional.of(Task.create(TaskId.of(2002L), "demo", TaskType.values()[0], ActivityId.of(3003L))));
        when(inventoryAppService.handleInventory(any())).thenReturn(
                VehicleInventoryApplicationService.InventoryHandlingResult.builder()
                        .disposition(InventoryDisposition.ACCEPTED).inventoryRevision(1L).build());
        when(availabilityService.evaluate(any(), any(), any())).thenReturn(
                new TaskAvailabilityService.AvailabilityResult(AvailabilityStatus.AVAILABLE, true, true, true));
        when(upgradeTargetMapper.selectPoByActivityId(3003L)).thenReturn(List.of());
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(1001L)).thenReturn(List.of());

        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin("HWYZTEST900000001");
        cmd.setInventoryMode("FULL");

        DetectionResult result = service.detect(cmd);

        assertEquals(1, result.getMatchedTasks().size());
        assertTrue(result.getMatchedTasks().get(0).getPackages().isEmpty());
    }
}
