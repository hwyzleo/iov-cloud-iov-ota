package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.SoftwareUnitCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskDetectionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PackageMetadataGateMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Task.VehicleTaskSnapshot;
import vehicle.fota.v1.Types.Digest;
import vehicle.fota.v1.Types.EcuSoftwareModel;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.PackageSummary;
import vehicle.fota.v1.Types.SoftwareUnitVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务检测命令处理器（CR-014 §5：vehicle.fota.v1.TaskCheckRequest）
 *
 * <p>清单握手（FULL/DIGEST）、任务匹配与本地任务对账（US-074）。
 * CR-019：解析 inventory_canonicalization_version、software_model/slot/active/
 * software_units，返回 supported_ecu_software_models / supported_inventory_canonicalization_versions。
 * CR-027：下发期包元数据门禁可由 {@code ota.task-check.package-metadata-gate.enabled=false}
 * 在联调环境显式豁免（package_size 不可豁免）。
 *
 * @author hwyz_leo
 */@Slf4j
@Component
public class TaskCheckCommandHandler {

    /** 云端支持的 ECU 软件模型（CR-019 §6.4） */
    private static final List<EcuSoftwareModel> SUPPORTED_MODELS = List.of(
            EcuSoftwareModel.ECU_SOFTWARE_MODEL_SINGLE_IMAGE,
            EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET);

    /** 云端支持的 canonicalization 版本 */
    private static final List<Integer> SUPPORTED_CANONICALIZATION_VERSIONS = List.of(1, 2);

    /** 云端支持的清单摘要算法（小写形式，与车端默认一致） */
    private static final List<String> SUPPORTED_DIGEST_ALGORITHMS = List.of("sha-256");

    /** 域内清单处置：已接受 */
    private static final String DISPOSITION_ACCEPTED = "ACCEPTED";

    /** 包清单为空时的拒绝原因（车端据此可定位，不得放行下载/安装） */
    private static final String DENY_NO_PACKAGE = "NO_PACKAGE_IN_TASK_SNAPSHOT";
    /** 包元数据不完整时的拒绝原因（IOV-OTA-DSN-CR-026 §7.5：禁止 0 字节下发） */
    private static final String DENY_METADATA_INCOMPLETE = "PACKAGE_METADATA_INCOMPLETE";

    private final TaskDetectionAppService taskDetectionAppService;

    private final PackageMetadataGateMetrics packageMetadataGateMetrics;

    /**
     * 下发期包元数据门禁开关（IOV-OTA-DSN-CR-027 §4.32.2）。
     *
     * <p>{@code true}（默认）=保持 CR-026 fail-closed；{@code false}=联调期豁免
     * sha256/签名/签名算法/证书/对象引用校验，{@code package_size} 仍不可豁免。
     */
    private final boolean packageMetadataGateEnabled;

    public TaskCheckCommandHandler(
            TaskDetectionAppService taskDetectionAppService,
            PackageMetadataGateMetrics packageMetadataGateMetrics,
            @Value("${ota.task-check.package-metadata-gate.enabled:true}")
            boolean packageMetadataGateEnabled) {
        this.taskDetectionAppService = taskDetectionAppService;
        this.packageMetadataGateMetrics = packageMetadataGateMetrics;
        this.packageMetadataGateEnabled = packageMetadataGateEnabled;
    }

    public TaskCheckResponse handle(FotaMessageMetadata md, TaskCheckRequest req) {
        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(md.vin());
        cmd.setInventoryMode(protoInventoryMode(req.getInventoryMode()));
        cmd.setInventoryRevision(req.getInventoryRevision());
        if (req.hasEcuListDigest()) {
            cmd.setDigestAlgorithm(req.getEcuListDigest().getAlgorithm());
            cmd.setInventoryDigest(req.getEcuListDigest().getValueHex());
        }
        // CR-019：canonicalization 版本 + 采集时间（Envelope.timestampMs）
        if (req.getInventoryCanonicalizationVersion() > 0) {
            cmd.setCanonicalizationVersion((int) req.getInventoryCanonicalizationVersion());
        }
        cmd.setCollectedAt(md.timestampMs());
        if (req.getEcuListCount() > 0) {
            cmd.setInventoryItems(req.getEcuListList().stream()
                    .map(TaskCheckCommandHandler::toInventoryItem).toList());
        }
        if (req.hasLocalTaskRevision()) {
            cmd.setLocalTaskRevision(req.getLocalTaskRevision());
        }

        DetectionResult result = taskDetectionAppService.detect(cmd);

        TaskCheckResponse.Builder b = TaskCheckResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setInventoryDisposition(FotaProtocols.inventoryDisposition(result.getInventoryDisposition()))
                .setAvailabilityStatus(FotaProtocols.availabilityStatus(result.getAvailabilityStatus()))
                .setDownloadAllowed(result.isDownloadAllowed())
                .setInstallRequestAllowed(result.isInstallRequestAllowed())
                .addAllSupportedInventoryDigestAlgorithms(SUPPORTED_DIGEST_ALGORITHMS)
                .addAllSupportedEcuSoftwareModels(SUPPORTED_MODELS)
                .addAllSupportedInventoryCanonicalizationVersions(SUPPORTED_CANONICALIZATION_VERSIONS);
        // ACCEPTED 必须回显云端实际受理的 revision，车端据此建立 DIGEST 基线
        // （缺失时车端只能回退本地 revision，云端重绑定时会持续 FULL 重试）。
        if (DISPOSITION_ACCEPTED.equals(result.getInventoryDisposition())
                && result.getAcceptedInventoryRevision() != null) {
            b.setAcceptedInventoryRevision(result.getAcceptedInventoryRevision());
        }
        if (result.getInventoryDisposition() != null && result.getInventoryDisposition().startsWith("FULL")) {
            b.setRequiredInventoryMode(vehicle.fota.v1.Types.InventoryMode.INVENTORY_MODE_FULL);
        }
        if (result.getMatchedTasks() != null && !result.getMatchedTasks().isEmpty()) {
            DetectionResult.MatchedVehicleTask t = result.getMatchedTasks().get(0);
            int packageCount = t.getPackages() == null ? 0 : t.getPackages().size();
            // 包清单是车端下载/安装的唯一依据：空包清单伴随 PROCEED 会让车端"空跑"到
            // WaitingWindow 后静默卡死（车端 doDownload() 空循环 → 假 READY）。
            // 因此空包清单不得放行下载/安装，也不得回 PROCEED。
            boolean hasPackages = packageCount > 0;
            // IOV-OTA-DSN-CR-026 §7.5：任一包元数据不完整（大小/摘要/签名/证书/对象）
            // 一律 fail-closed，既不设 task 快照也不放行下载，禁止把 NULL 大小下发成 0。
            // IOV-OTA-DSN-CR-027 §4.32：联调期可显式关闭下发期门禁
            // （ota.task-check.package-metadata-gate.enabled=false），但 package_size
            // 不可豁免：0/NULL 大小会让车端 mock 进度事实失真；发布期与包登记期门禁不受影响。
            List<String> incompleteReasonsList = new ArrayList<>();
            boolean blockedBySize = false;
            if (hasPackages) {
                for (DetectionResult.MatchedPackage p : t.getPackages()) {
                    boolean sizeInvalid = p.getSizeBytes() == null || p.getSizeBytes() <= 0;
                    boolean metadataIncomplete = !Boolean.TRUE.equals(p.getMetadataComplete());
                    if (sizeInvalid) {
                        blockedBySize = true;
                    }
                    if (sizeInvalid || metadataIncomplete) {
                        String reasons = p.getMetadataIncompleteReasons() == null
                                ? "PACKAGE_SIZE_MISSING_OR_INVALID" : p.getMetadataIncompleteReasons();
                        incompleteReasonsList.add(p.getPackageId() + ":" + reasons);
                    }
                }
            }
            String incompleteReasons = String.join(";", incompleteReasonsList);
            // 豁免仅跳过非 size 原因；存在非法大小时仍然阻断
            boolean metadataBypassed = !packageMetadataGateEnabled
                    && !incompleteReasonsList.isEmpty() && !blockedBySize;
            if (metadataBypassed) {
                packageMetadataGateMetrics.bypassed(incompleteReasons);
                log.warn("车辆任务[{}]包元数据门禁已豁免（联调开关 ota.task-check.package-metadata-gate.enabled=false），"
                                + "taskId[{}] revision[{}] packageCount[{}] reasons[{}]",
                        t.getVehicleTaskId(), t.getTaskId(), t.getTaskRevision(),
                        packageCount, incompleteReasons);
            }
            boolean actionable = hasPackages
                    && (incompleteReasonsList.isEmpty() || metadataBypassed);
            b.setSnapshotChanged(t.isSnapshotChanged())
                    .setReconsentRequired(t.isReconsentRequired())
                    .setLocalTaskDisposition(t.getLocalDisposition() == null ? "" : t.getLocalDisposition())
                    .setPackageCacheAction(t.getPackageCacheAction() == null ? "" : t.getPackageCacheAction())
                    .setDownloadAllowed(result.isDownloadAllowed() && actionable)
                    .setInstallRequestAllowed(result.isInstallRequestAllowed() && actionable)
                    .setNextAction(actionable ? "PROCEED" : "WAIT");
            if (actionable) {
                b.setTask(toSnapshot(t));
                log.info("车辆任务[{}]下发任务快照：taskId[{}] activityId[{}] revision[{}] packageCount[{}] installPlanVersion[{}]",
                        t.getVehicleTaskId(), t.getTaskId(), t.getActivityId(), t.getTaskRevision(),
                        packageCount, t.getInstallPlanVersion());
            } else if (!hasPackages) {
                b.setDownloadDenyReason(DENY_NO_PACKAGE).setInstallRequestDenyReason(DENY_NO_PACKAGE);
                log.error("车辆任务[{}]包清单为空，拒绝放行下载/安装：taskId[{}] activityId[{}] revision[{}] installPlanVersion[{}]",
                        t.getVehicleTaskId(), t.getTaskId(), t.getActivityId(), t.getTaskRevision(),
                        t.getInstallPlanVersion());
            } else {
                b.setDownloadDenyReason(DENY_METADATA_INCOMPLETE)
                        .setInstallRequestDenyReason(DENY_METADATA_INCOMPLETE);
                log.error("车辆任务[{}]包元数据不完整，拒绝放行下载/安装：taskId[{}] revision[{}] reasons[{}]",
                        t.getVehicleTaskId(), t.getTaskId(), t.getTaskRevision(), incompleteReasons);
            }
        } else {
            b.setNextAction("WAIT");
        }
        return b.build();
    }

    /**
     * 将领域匹配结果投影为协议任务快照（CR-014 §5）。
     *
     * <p>必须投影 activity_id / install_plan_version / package_list：
     * 车端 doDownload() 按 package_list 迭代申请下载凭证，缺失即整个 FOTA 流程无法推进。</p>
     */
    private static VehicleTaskSnapshot toSnapshot(DetectionResult.MatchedVehicleTask t) {
        VehicleTaskSnapshot.Builder s = VehicleTaskSnapshot.newBuilder()
                .setVehicleTaskId(String.valueOf(t.getVehicleTaskId()))
                .setTaskRevision(t.getTaskRevision() == null ? 0L : t.getTaskRevision())
                .setReleaseAtMs(toEpochMilli(t.getReleaseAt()))
                .setStartTimeMs(toEpochMilli(t.getStartTime()))
                .setEndTimeMs(toEpochMilli(t.getEndTime()))
                .setConsentRequired(t.isReconsentRequired())
                .setInstallPlanVersion(t.getInstallPlanVersion() == null ? "" : t.getInstallPlanVersion());
        if (t.getActivityId() != null) {
            s.setActivityId(String.valueOf(t.getActivityId()));
        }
        if (t.getSnapshotDigest() != null) {
            s.setTargetBaselineCode(t.getSnapshotDigest());
        }
        if (t.getPackages() != null) {
            for (DetectionResult.MatchedPackage p : t.getPackages()) {
                PackageSummary.Builder ps = PackageSummary.newBuilder()
                        .setPackageId(nullToEmpty(p.getPackageId()))
                        .setPackageRevision(nullToEmpty(p.getPackageRevision()))
                        .setPlanNodeId(nullToEmpty(p.getPlanNodeId()))
                        .setEcuId(nullToEmpty(p.getEcuId()))
                        .setComponentId(nullToEmpty(p.getComponentId()))
                        .setSourceVersion(nullToEmpty(p.getSourceVersion()))
                        .setTargetVersion(nullToEmpty(p.getTargetVersion()))
                        .setSizeBytes(p.getSizeBytes() == null ? 0L : p.getSizeBytes())
                        .setDifferentialType(nullToEmpty(p.getDifferentialType()))
                        .setEncrypted(Boolean.TRUE.equals(p.getEncrypted()));
                if (p.getDigestHex() != null && !p.getDigestHex().isBlank()) {
                    ps.setPackageDigest(Digest.newBuilder()
                            .setAlgorithm(p.getDigestAlgorithm() == null || p.getDigestAlgorithm().isBlank()
                                    ? "sha-256" : p.getDigestAlgorithm())
                            .setValueHex(p.getDigestHex())
                            .build());
                }
                s.addPackageList(ps.build());
            }
        }
        return s.build();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static long toEpochMilli(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private static InventoryItemCmd toInventoryItem(EcuVersion ecu) {
        List<SoftwareUnitCmd> units = new ArrayList<>();
        if (ecu.getSoftwareUnitsCount() > 0) {
            for (SoftwareUnitVersion u : ecu.getSoftwareUnitsList()) {
                units.add(SoftwareUnitCmd.builder()
                        .softwareTargetCode(u.getSoftwareTargetCode())
                        .softwarePartNumber(u.getSoftwarePartNumber())
                        .swVersion(u.getSwVersion())
                        .slot(u.hasSlot() ? u.getSlot() : null)
                        .active(u.hasActive() ? u.getActive() : null)
                        .digest(u.hasSoftwareDigest() ? u.getSoftwareDigest().getValueHex() : null)
                        .build());
            }
        }
        return InventoryItemCmd.builder()
                .ecuId(ecu.getEcuId())
                .softwarePn(ecu.getSoftwarePartNumber())
                .softwareVersion(ecu.getSwVersion())
                .hardwarePn(ecu.getHardwarePartNumber())
                .hardwareVersion(ecu.getHwVersion())
                .softwareModel(protoSoftwareModel(ecu.getSoftwareModel()))
                .slot(ecu.hasSlot() ? ecu.getSlot() : null)
                .active(ecu.hasActive() ? ecu.getActive() : null)
                .softwareUnits(units)
                .build();
    }

    private static String protoSoftwareModel(EcuSoftwareModel model) {
        if (model == null) {
            return null;
        }
        return switch (model) {
            case ECU_SOFTWARE_MODEL_SINGLE_IMAGE -> "SINGLE_IMAGE";
            case ECU_SOFTWARE_MODEL_MULTI_TARGET -> "MULTI_TARGET";
            default -> null;
        };
    }

    private static String protoInventoryMode(vehicle.fota.v1.Types.InventoryMode mode) {
        return switch (mode) {
            case INVENTORY_MODE_FULL -> "FULL";
            case INVENTORY_MODE_DIGEST -> "DIGEST";
            default -> null;
        };
    }
}
