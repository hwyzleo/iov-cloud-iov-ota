package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AvailabilityStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskAvailabilityService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 任务检测应用服务（CR-012 §5.1、US-074）
 *
 * <p>清单握手（FULL/DIGEST）、任务匹配和本地任务对账。
 * 任务选择只读取发布时冻结的 VehicleTask，不在检测时重新圈车。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetectionAppService {

    private final VehicleTaskRepository vehicleTaskRepository;
    private final TaskRepository taskRepository;
    private final VehicleInventoryMapper vehicleInventoryMapper;
    private final VehicleInventoryItemMapper vehicleInventoryItemMapper;
    private final TaskAvailabilityService taskAvailabilityService;

    /**
     * 检测任务：清单握手 + 任务匹配 + 可用性计算。
     *
     * @param cmd 检测命令
     * @return 检测结果
     */
    @Transactional
    public DetectionResult detect(DetectionCmd cmd) {
        log.info("车辆[{}]检测任务，清单模式[{}]", cmd.getVin(), cmd.getInventoryMode());

        // 1. 清单握手
        InventoryDisposition inventoryDisposition = handleInventory(cmd);

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
                .matchedTasks(matched)
                .build();
    }

    /**
     * 处理清单握手。
     */
    private InventoryDisposition handleInventory(DetectionCmd cmd) {
        if ("FULL".equals(cmd.getInventoryMode())) {
            return handleFullInventory(cmd);
        } else if ("DIGEST".equals(cmd.getInventoryMode())) {
            return handleDigestInventory(cmd);
        }
        return InventoryDisposition.FULL_REQUIRED;
    }

    /**
     * FULL 清单：同事务保存清单头、明细和摘要。
     */
    private InventoryDisposition handleFullInventory(DetectionCmd cmd) {
        if (cmd.getInventoryItems() == null || cmd.getInventoryItems().isEmpty()) {
            return InventoryDisposition.FULL_REQUIRED;
        }
        long revision = cmd.getInventoryRevision() != null ? cmd.getInventoryRevision() : 1L;

        VehicleInventoryPo header = VehicleInventoryPo.builder()
                .vin(cmd.getVin())
                .inventoryRevision(revision)
                .digest(cmd.getInventoryDigest())
                .algorithm(cmd.getDigestAlgorithm() != null ? cmd.getDigestAlgorithm() : "SHA-256")
                .acceptedTime(java.util.Date.from(Instant.now()))
                .build();
        vehicleInventoryMapper.insert(header);

        for (InventoryItemCmd item : cmd.getInventoryItems()) {
            VehicleInventoryItemPo itemPo = VehicleInventoryItemPo.builder()
                    .inventoryId(header.getId())
                    .ecuId(item.getEcuId())
                    .ecuName(item.getEcuName())
                    .softwarePn(item.getSoftwarePn())
                    .softwareVersion(item.getSoftwareVersion())
                    .hardwarePn(item.getHardwarePn())
                    .hardwareVersion(item.getHardwareVersion())
                    .build();
            vehicleInventoryItemMapper.insert(itemPo);
        }
        log.info("车辆[{}]FULL清单已接受，版本[{}]", cmd.getVin(), revision);
        return InventoryDisposition.ACCEPTED;
    }

    /**
     * DIGEST 清单：必须命中同 VIN、同 revision 的完整清单。
     */
    private InventoryDisposition handleDigestInventory(DetectionCmd cmd) {
        VehicleInventoryPo existing = vehicleInventoryMapper.selectLatestByVin(cmd.getVin());
        if (existing == null) {
            return InventoryDisposition.FULL_REQUIRED;
        }
        if (cmd.getInventoryRevision() != null
                && !cmd.getInventoryRevision().equals(existing.getInventoryRevision())) {
            return InventoryDisposition.REVISION_CONFLICT;
        }
        if (cmd.getInventoryDigest() != null && !cmd.getInventoryDigest().equals(existing.getDigest())) {
            return InventoryDisposition.DIGEST_MISMATCH;
        }
        if (cmd.getDigestAlgorithm() != null && !cmd.getDigestAlgorithm().equals(existing.getAlgorithm())) {
            return InventoryDisposition.ALGORITHM_UNSUPPORTED;
        }
        return InventoryDisposition.ACCEPTED;
    }

    private boolean isSnapshotChanged(VehicleTask vt, Long localTaskRevision) {
        if (localTaskRevision == null) {
            return true;
        }
        return vt.getTaskRevision().getValue() != localTaskRevision;
    }
}
