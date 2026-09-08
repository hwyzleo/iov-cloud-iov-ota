package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.SoftwareUnitCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskDetectionAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Task.VehicleTaskSnapshot;
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
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class TaskCheckCommandHandler {

    /** 云端支持的 ECU 软件模型（CR-019 §6.4） */
    private static final List<EcuSoftwareModel> SUPPORTED_MODELS = List.of(
            EcuSoftwareModel.ECU_SOFTWARE_MODEL_SINGLE_IMAGE,
            EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET);

    /** 云端支持的 canonicalization 版本 */
    private static final List<Integer> SUPPORTED_CANONICALIZATION_VERSIONS = List.of(1, 2);

    private final TaskDetectionAppService taskDetectionAppService;

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
                .addAllSupportedEcuSoftwareModels(SUPPORTED_MODELS)
                .addAllSupportedInventoryCanonicalizationVersions(SUPPORTED_CANONICALIZATION_VERSIONS);
        if (result.getInventoryDisposition() != null && result.getInventoryDisposition().startsWith("FULL")) {
            b.setRequiredInventoryMode(vehicle.fota.v1.Types.InventoryMode.INVENTORY_MODE_FULL);
        }
        if (result.getMatchedTasks() != null && !result.getMatchedTasks().isEmpty()) {
            DetectionResult.MatchedVehicleTask t = result.getMatchedTasks().get(0);
            b.setSnapshotChanged(t.isSnapshotChanged())
                    .setReconsentRequired(t.isReconsentRequired())
                    .setLocalTaskDisposition(t.getLocalDisposition() == null ? "" : t.getLocalDisposition())
                    .setPackageCacheAction(t.getPackageCacheAction() == null ? "" : t.getPackageCacheAction())
                    .setNextAction("PROCEED")
                    .setTask(toSnapshot(t));
        } else {
            b.setNextAction("WAIT");
        }
        return b.build();
    }

    private static VehicleTaskSnapshot toSnapshot(DetectionResult.MatchedVehicleTask t) {
        VehicleTaskSnapshot.Builder s = VehicleTaskSnapshot.newBuilder()
                .setVehicleTaskId(String.valueOf(t.getVehicleTaskId()))
                .setTaskRevision(t.getTaskRevision() == null ? 0L : t.getTaskRevision())
                .setReleaseAtMs(toEpochMilli(t.getReleaseAt()))
                .setStartTimeMs(toEpochMilli(t.getStartTime()))
                .setEndTimeMs(toEpochMilli(t.getEndTime()))
                .setConsentRequired(t.isReconsentRequired())
                .setInstallPlanVersion("");
        if (t.getSnapshotDigest() != null) {
            s.setTargetBaselineCode(t.getSnapshotDigest());
        }
        return s.build();
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
