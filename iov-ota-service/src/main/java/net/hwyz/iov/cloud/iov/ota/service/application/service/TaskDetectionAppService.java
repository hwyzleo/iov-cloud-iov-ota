package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.FotaTaskMatcher;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskAvailabilityService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务检测应用服务（CR-012 §5.1、US-074）
 *
 * <p>清单握手（FULL/DIGEST）委托 {@link VehicleInventoryApplicationService}（CR-019 统一
 * Canonical Inventory 受理），随后做任务匹配和本地任务对账。
 * 任务选择只读取发布时冻结的 VehicleTask，不在检测时重新圈车；
 * 可见任务按 Canonical Inventory 的 ECU + Target + PN/version 过滤（CR-019 §6.3）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetectionAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final TaskRepository taskRepository;
    private final VehicleInventoryApplicationService vehicleInventoryApplicationService;
    private final TaskAvailabilityService taskAvailabilityService;
    private final VehicleInventoryMapper vehicleInventoryMapper;
    private final VehicleInventoryItemMapper vehicleInventoryItemMapper;
    private final VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    private final ActivityUpgradeTargetMapper activityUpgradeTargetMapper;

    /**
     * 检测任务：清单握手 + 任务匹配 + 可用性计算。
     *
     * @param cmd 检测命令
     * @return 检测结果
     */
    @Transactional
    public DetectionResult detect(DetectionCmd cmd) {
        log.info("车辆[{}]检测任务，清单模式[{}]", cmd.getVin(), cmd.getInventoryMode());

        // 1. 清单握手（CR-019：统一 Canonical Inventory 受理）
        VehicleInventoryApplicationService.InventoryHandlingResult inventoryResult =
                vehicleInventoryApplicationService.handleInventory(cmd);
        InventoryDisposition inventoryDisposition = inventoryResult.getDisposition();

        // 清单不可用时，不返回可下载/可安装任务
        if (inventoryDisposition != InventoryDisposition.ACCEPTED) {
            return DetectionResult.builder()
                    .inventoryDisposition(inventoryDisposition.getValue())
                    .availabilityStatus(AvailabilityStatus.NONE.getValue())
                    .visible(false)
                    .downloadAllowed(false)
                    .installRequestAllowed(false)
                    .matchedTasks(List.of())
                    .build();
        }

        // 2. 匹配可见 VehicleTask
        List<VehicleTask> visibleTasks = vehicleTaskRepository.findVisibleByVin(cmd.getVin());
        Instant now = Instant.now();

        // CR-019：读取已接受 Canonical Inventory（按 SoftwareUnit 过滤可见任务）
        CanonicalVehicleInventory canonical = loadAcceptedInventory(cmd.getVin());

        List<DetectionResult.MatchedVehicleTask> matched = new ArrayList<>();
        boolean anyVisible = false;
        boolean anyDownloadAllowed = false;
        boolean anyInstallAllowed = false;
        AvailabilityStatus worstStatus = AvailabilityStatus.NONE;

        for (VehicleTask vt : visibleTasks) {
            if (vt.isTerminal()) {
                continue;
            }
            Optional<Task> taskOpt = taskRepository.getById(TaskId.of(vt.getTaskId()));
            if (taskOpt.isEmpty()) {
                continue;
            }
            Task task = taskOpt.get();

            // CR-019 §6.3：按 ECU + Target + PN/version 过滤可见任务
            if (canonical != null && !isTaskMatched(canonical, task)) {
                log.debug("车辆[{}]任务[{}]未匹配 Canonical Inventory，跳过", cmd.getVin(), task.getId().getValue());
                continue;
            }

            TaskAvailabilityService.AvailabilityResult avail =
                    taskAvailabilityService.evaluate(task, vt, now);

            if (avail.isVisible()) {
                anyVisible = true;
            }
            if (avail.isDownloadAllowed()) {
                anyDownloadAllowed = true;
            }
            if (avail.isInstallRequestAllowed()) {
                anyInstallAllowed = true;
            }
            // 取最具体的可用性状态
            if (avail.getAvailabilityStatus() != AvailabilityStatus.NONE) {
                worstStatus = avail.getAvailabilityStatus();
            }

            matched.add(DetectionResult.MatchedVehicleTask.builder()
                    .vehicleTaskId(vt.getId().getValue())
                    .taskId(vt.getTaskId())
                    .taskRevision(vt.getTaskRevision().getValue())
                    .snapshotDigest(vt.getSnapshotDigest() != null ? vt.getSnapshotDigest().getValue() : null)
                    .snapshotChanged(isSnapshotChanged(vt, cmd.getLocalTaskRevision()))
                    .reconsentRequired(false)
                    .localDisposition(vt.getLocalDisposition())
                    .packageCacheAction(vt.getPackageCacheAction())
                    .releaseAt(vt.getReleaseAt())
                    .startTime(vt.getStartTime())
                    .endTime(vt.getEndTime())
                    .build());
        }

        return DetectionResult.builder()
                .inventoryDisposition(inventoryDisposition.getValue())
                .availabilityStatus(worstStatus.getValue())
                .visible(anyVisible)
                .downloadAllowed(anyDownloadAllowed)
                .installRequestAllowed(anyInstallAllowed)
                .inventoryModel(inventoryResult.getInventoryModel())
                .canonicalizationVersion(inventoryResult.getCanonicalizationVersion())
                .canonicalDigestHex(inventoryResult.getCanonicalDigestHex())
                .matchedTasks(matched)
                .build();
    }

    private boolean isSnapshotChanged(VehicleTask vt, Long localTaskRevision) {
        if (localTaskRevision == null) {
            return true;
        }
        return vt.getTaskRevision().getValue() != localTaskRevision;
    }

    /**
     * 读取已接受 Canonical Inventory（按 SoftwareUnit 重建）。
     */
    private CanonicalVehicleInventory loadAcceptedInventory(String vin) {
        VehicleInventoryPo header = vehicleInventoryMapper.selectLatestByVin(vin);
        if (header == null) {
            return null;
        }
        List<VehicleInventoryItemPo> items = vehicleInventoryItemMapper.selectByInventoryId(header.getId());
        List<CanonicalEcu> ecus = new ArrayList<>();
        for (VehicleInventoryItemPo item : items) {
            List<VehicleInventorySoftwareUnitPo> unitPos =
                    vehicleInventorySoftwareUnitMapper.selectByInventoryItemId(item.getId());
            List<CanonicalSoftwareUnit> units = unitPos.stream()
                    .map(u -> new CanonicalSoftwareUnit(
                            u.getSoftwareTargetCode(),
                            u.getSoftwarePartNumber(),
                            u.getSwVersion(),
                            u.getSlot(),
                            u.getActive() == null || u.getActive(),
                            u.getDigest()))
                    .toList();
            ecus.add(CanonicalEcu.builder()
                    .ecuId(item.getEcuId())
                    .hardwarePartNumber(item.getHardwarePn())
                    .hwVersion(item.getHardwareVersion())
                    .softwareUnits(units)
                    .build());
        }
        return CanonicalVehicleInventory.builder()
                .vin(vin)
                .inventoryRevision(header.getInventoryRevision())
                .canonicalizationVersion(header.getCanonicalizationVersion() == null
                        ? 1 : header.getCanonicalizationVersion())
                .ecuList(ecus)
                .build();
    }

    /**
     * CR-019 §6.3：按活动升级对象声明的 ECU + Target + PN 过滤任务。
     * 活动未声明升级对象时保持兼容（不额外过滤）。
     */
    private boolean isTaskMatched(CanonicalVehicleInventory canonical, Task task) {
        if (task.getActivityId() == null) {
            return true;
        }
        List<ActivityUpgradeTargetPo> targets = activityUpgradeTargetMapper
                .selectPoByActivityId(task.getActivityId().getValue());
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        List<FotaTaskMatcher.UpgradeRequirement> requirements = targets.stream()
                .map(t -> new FotaTaskMatcher.UpgradeRequirement(
                        t.getVehicleNodeCode(),
                        t.getSoftwareTargetCode(),
                        t.getPartCode(),
                        null,
                        null))
                .collect(Collectors.toList());
        return FotaTaskMatcher.isEligible(canonical, requirements);
    }
}
