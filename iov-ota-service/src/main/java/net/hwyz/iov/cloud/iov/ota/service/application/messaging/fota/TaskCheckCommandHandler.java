package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskDetectionAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Task.VehicleTaskSnapshot;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.PackageSummary;

import java.time.Instant;
import java.util.List;

/**
 * 任务检测命令处理器（CR-014 §5：vehicle.fota.v1.TaskCheckRequest）
 *
 * <p>清单握手（FULL/DIGEST）、任务匹配与本地任务对账（US-074）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class TaskCheckCommandHandler {

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
                .setInstallRequestAllowed(result.isInstallRequestAllowed());
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
        return InventoryItemCmd.builder()
                .ecuId(ecu.getEcuId())
                .softwarePn(ecu.getSoftwarePartNumber())
                .softwareVersion(ecu.getSwVersion())
                .hardwarePn(ecu.getHardwarePartNumber())
                .hardwareVersion(ecu.getHwVersion())
                .build();
    }

    private static String protoInventoryMode(vehicle.fota.v1.Types.InventoryMode mode) {
        return switch (mode) {
            case INVENTORY_MODE_FULL -> "FULL";
            case INVENTORY_MODE_DIGEST -> "DIGEST";
            default -> null;
        };
    }
}
