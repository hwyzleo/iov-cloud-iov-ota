package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalEcu;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CanonicalVehicleInventory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.CanonicalSoftwareUnit;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 车辆任务包清单投影（CCU-FOTA 协议 PackageSummary 的云端唯一投影来源）。
 *
 * <p>数据源链路：<code>tb_activity_upgrade_target</code> →
 * <code>tb_software_build_version</code> → <code>tb_software_build_version_package</code> →
 * <code>tb_software_package</code>；若 <code>tb_vehicle_task_package</code> 已冻结包集合，则以冻结集合为准。</p>
 *
 * <p>该投影同时被两处消费，必须是同一份事实：</p>
 * <ul>
 *   <li>{@link TaskDetectionAppService}：TaskCheckResponse.task.package_list（车端下载依据）；</li>
 *   <li>{@link PackageDeliveryAppService}：判定"全部必需包已下载/校验完成"，
 *       决定 VehicleTask 是否可进入 READY_TO_INSTALL（不能只看已上报结果）。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTaskPackageService {

    private final TaskRepository taskRepository;
    private final ActivityUpgradeTargetMapper activityUpgradeTargetMapper;
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper;
    private final SoftwareBuildVersionMapper softwareBuildVersionMapper;
    private final SoftwareBuildVersionPackageMapper softwareBuildVersionPackageMapper;
    private final SoftwarePackageMapper softwarePackageMapper;

    /**
     * 投影任务包清单（带 ECU/版本/大小/摘要，用于下发）。
     *
     * @param vt        车辆任务
     * @param task      任务（提供活动ID）
     * @param canonical 已受理 Canonical Inventory（可为 null，用于回填 source_version）
     */
    public List<DetectionResult.MatchedPackage> project(VehicleTask vt, Task task,
                                                        CanonicalVehicleInventory canonical) {
        if (vt == null || task == null || task.getActivityId() == null) {
            return List.of();
        }
        List<ActivityUpgradeTargetPo> targets =
                activityUpgradeTargetMapper.selectPoByActivityId(task.getActivityId().getValue());
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        Set<String> frozenPackageIds = vehicleTaskPackageMapper
                .selectByVehicleTaskId(vt.getId().getValue()).stream()
                .map(VehicleTaskPackagePo::getPackageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, DetectionResult.MatchedPackage> byPackageId = new LinkedHashMap<>();
        for (ActivityUpgradeTargetPo target : targets) {
            if (target.getSoftwareBuildVersionId() == null) {
                continue;
            }
            SoftwareBuildVersionPo buildVersion =
                    softwareBuildVersionMapper.selectPoById(target.getSoftwareBuildVersionId());
            if (buildVersion == null) {
                log.warn("活动[{}]升级对象[{}]软件内部版本[{}]不存在，包清单剔除",
                        task.getActivityId().getValue(), target.getVehicleNodeCode(),
                        target.getSoftwareBuildVersionId());
                continue;
            }
            List<SoftwareBuildVersionPackagePo> links = softwareBuildVersionPackageMapper
                    .selectPoBySoftwareBuildVersionId(target.getSoftwareBuildVersionId());
            if (links == null || links.isEmpty()) {
                log.warn("软件内部版本[{}]未关联软件包，包清单剔除（节点[{}]）",
                        target.getSoftwareBuildVersionId(), target.getVehicleNodeCode());
                continue;
            }
            for (SoftwareBuildVersionPackagePo link : links) {
                if (link.getSoftwarePackageId() == null) {
                    continue;
                }
                SoftwarePackagePo pkg = softwarePackageMapper.selectPoById(link.getSoftwarePackageId());
                if (pkg == null) {
                    continue;
                }
                String packageId = pkg.getPackageCode() != null && !pkg.getPackageCode().isBlank()
                        ? pkg.getPackageCode() : String.valueOf(pkg.getId());
                if (!frozenPackageIds.isEmpty() && !frozenPackageIds.contains(packageId)) {
                    continue;
                }
                String componentId = link.getSoftwareTargetCode() != null
                        ? link.getSoftwareTargetCode() : target.getSoftwareTargetCode();
                byPackageId.putIfAbsent(packageId, DetectionResult.MatchedPackage.builder()
                        .packageId(packageId)
                        .packageRevision(buildVersion.getSoftwareBuildVer())
                        .planNodeId("")
                        .ecuId(target.getVehicleNodeCode())
                        .componentId(componentId)
                        .sourceVersion(sourceVersion(canonical, target.getVehicleNodeCode(), componentId))
                        .targetVersion(buildVersion.getSoftwareBuildVer())
                        .sizeBytes(pkg.getPackageSize())
                        .digestAlgorithm("sha-256")
                        .digestHex(pkg.getPackageSha256())
                        .differentialType("")
                        .encrypted(Boolean.FALSE)
                        .build());
            }
        }
        return new ArrayList<>(byPackageId.values());
    }

    /**
     * 该车辆任务的**必需包 ID 集合**（与下发给车端的 package_list 同源）。
     *
     * <p>用于"全部必需包已完成"判定：只看已上报结果会把"部分包"误判为全部完成
     * （实车 2026-11-12：第 1 个包 DECRYPT SUCCESS 后任务被提前置为 READY_TO_INSTALL，
     * 后续包的下载授权全部因状态校验异常而不回执）。</p>
     */
    public Set<String> requiredPackageIds(VehicleTask vt) {
        if (vt == null || vt.getTaskId() == null) {
            return Set.of();
        }
        Optional<Task> taskOpt = taskRepository.getById(
                net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId.of(vt.getTaskId()));
        if (taskOpt.isEmpty()) {
            log.warn("车辆任务[{}]对应任务[{}]不存在，无法投影必需包集合", vt.getId().getValue(), vt.getTaskId());
            return Set.of();
        }
        return project(vt, taskOpt.get(), null).stream()
                .map(DetectionResult.MatchedPackage::getPackageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 从已受理 Canonical Inventory 回填包所在软件单元的当前版本（车端用作 source_version）。 */
    private String sourceVersion(CanonicalVehicleInventory canonical, String ecuId, String softwareTargetCode) {
        if (canonical == null || ecuId == null) {
            return "";
        }
        for (CanonicalEcu ecu : canonical.getEcuList()) {
            if (!ecuId.equalsIgnoreCase(ecu.getEcuId())) {
                continue;
            }
            if (ecu.getSoftwareUnits() == null) {
                return "";
            }
            for (CanonicalSoftwareUnit unit : ecu.getSoftwareUnits()) {
                if (softwareTargetCode == null || softwareTargetCode.isBlank()
                        || softwareTargetCode.equalsIgnoreCase(unit.softwareTargetCode())) {
                    return unit.swVersion() == null ? "" : unit.swVersion();
                }
            }
        }
        return "";
    }
}
