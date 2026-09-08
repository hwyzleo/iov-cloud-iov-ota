package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskDetectionAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Types.Digest;
import vehicle.fota.v1.Types.EcuSoftwareModel;
import vehicle.fota.v1.Types.EcuVersion;
import vehicle.fota.v1.Types.InventoryDisposition;
import vehicle.fota.v1.Types.InventoryMode;
import vehicle.fota.v1.Types.SoftwareUnitVersion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务检测命令处理器映射单测（CR-014 §5：TaskCheckRequest → DetectionCmd → TaskCheckResponse）
 *
 * @author hwyz_leo
 */
@DisplayName("TaskCheckCommandHandler - proto → 命令 → 响应映射")
class TaskCheckCommandHandlerTest {

    private final TaskDetectionAppService appService = mock(TaskDetectionAppService.class);
    private final TaskCheckCommandHandler handler = new TaskCheckCommandHandler(appService);
    private final FotaMessageMetadata md = new FotaMessageMetadata(
            "req-1", 1000L, "fota-v1", "dev-1", "LSVAU2188N2ZG4G",
            null, null, null, "vehicle.fota.v1.TaskCheckRequest",
            "msg-1", null, MessageKind.MESSAGE_KIND_REQUEST, null, null);

    @Test
    @DisplayName("FULL 清单 + ecu_list → DetectionCmd 字段映射正确 → ACCEPTED 响应")
    void full_inventory_maps_and_responds() {
        TaskCheckRequest req = TaskCheckRequest.newBuilder()
                .setInventoryMode(InventoryMode.INVENTORY_MODE_FULL)
                .setInventoryRevision(7L)
                .addEcuList(EcuVersion.newBuilder()
                        .setEcuId("ECU1").setSoftwarePartNumber("SPN1").setSwVersion("V1.0")
                        .setHardwarePartNumber("HPN1").setHwVersion("H1").build())
                .build();
        when(appService.detect(any())).thenReturn(DetectionResult.builder()
                .inventoryDisposition("ACCEPTED")
                .availabilityStatus("AVAILABLE")
                .downloadAllowed(true)
                .installRequestAllowed(true)
                .matchedTasks(List.of(DetectionResult.MatchedVehicleTask.builder()
                        .vehicleTaskId(1001L).taskRevision(3L)
                        .snapshotChanged(false).reconsentRequired(false)
                        .localDisposition("KEEP").packageCacheAction("KEEP")
                        .build()))
                .build());

        TaskCheckResponse resp = handler.handle(md, req);

        assertEquals(InventoryDisposition.INVENTORY_DISPOSITION_ACCEPTED, resp.getInventoryDisposition());
        assertTrue(resp.getDownloadAllowed());
        assertTrue(resp.getInstallRequestAllowed());
        assertEquals("PROCEED", resp.getNextAction());
        assertTrue(resp.hasTask());
        assertEquals("1001", resp.getTask().getVehicleTaskId());

        // 命令字段校验
        DetectionCmd cmd = argumentCaptor();
        assertEquals("LSVAU2188N2ZG4G", cmd.getVin());
        assertEquals("FULL", cmd.getInventoryMode());
        assertEquals(7L, cmd.getInventoryRevision());
        assertEquals(1, cmd.getInventoryItems().size());
        InventoryItemCmd item = cmd.getInventoryItems().get(0);
        assertEquals("ECU1", item.getEcuId());
        assertEquals("SPN1", item.getSoftwarePn());
        assertEquals("V1.0", item.getSoftwareVersion());
    }

    @Test
    @DisplayName("DIGEST 模式摘要映射")
    void digest_maps_digest_fields() {        TaskCheckRequest req = TaskCheckRequest.newBuilder()
                .setInventoryMode(InventoryMode.INVENTORY_MODE_DIGEST)
                .setInventoryRevision(3L)
                .setEcuListDigest(Digest.newBuilder().setAlgorithm("sha256").setValueHex("abc123").build())
                .build();
        when(appService.detect(any())).thenReturn(DetectionResult.builder()
                .inventoryDisposition("DIGEST_MISMATCH").availabilityStatus("NONE")
                .downloadAllowed(false).installRequestAllowed(false).build());

        TaskCheckResponse resp = handler.handle(md, req);

        DetectionCmd cmd = argumentCaptor();
        assertEquals("DIGEST", cmd.getInventoryMode());
        assertEquals("sha256", cmd.getDigestAlgorithm());
        assertEquals("abc123", cmd.getInventoryDigest());
        assertEquals(InventoryDisposition.INVENTORY_DISPOSITION_DIGEST_MISMATCH, resp.getInventoryDisposition());
    }

    @Test
    @DisplayName("MULTI_TARGET FULL 清单 → software_units 映射 + canonicalization 版本传递")
    void multi_target_maps_software_units() {
        TaskCheckRequest req = TaskCheckRequest.newBuilder()
                .setInventoryMode(InventoryMode.INVENTORY_MODE_FULL)
                .setInventoryRevision(2L)
                .setInventoryCanonicalizationVersion(2)
                .addEcuList(EcuVersion.newBuilder()
                        .setEcuId("ECU1")
                        .setSoftwareModel(EcuSoftwareModel.ECU_SOFTWARE_MODEL_MULTI_TARGET)
                        .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                                .setSoftwareTargetCode("BOOT").setSoftwarePartNumber("SPN-B").setSwVersion("V1.0"))
                        .addSoftwareUnits(SoftwareUnitVersion.newBuilder()
                                .setSoftwareTargetCode("APP").setSoftwarePartNumber("SPN-A").setSwVersion("V2.0")
                                .setSlot("A").setActive(true)))
                .build();
        when(appService.detect(any())).thenReturn(DetectionResult.builder()
                .inventoryDisposition("ACCEPTED")
                .inventoryModel("MULTI_TARGET")
                .canonicalizationVersion(2)
                .availabilityStatus("NONE")
                .downloadAllowed(false)
                .installRequestAllowed(false)
                .build());

        TaskCheckResponse resp = handler.handle(md, req);

        DetectionCmd cmd = argumentCaptor();
        assertEquals(2, cmd.getCanonicalizationVersion());
        assertEquals(1, cmd.getInventoryItems().size());
        assertEquals("MULTI_TARGET", cmd.getInventoryItems().get(0).getSoftwareModel());
        assertEquals(2, cmd.getInventoryItems().get(0).getSoftwareUnits().size());
        assertEquals("BOOT", cmd.getInventoryItems().get(0).getSoftwareUnits().get(0).getSoftwareTargetCode());
        assertEquals("A", cmd.getInventoryItems().get(0).getSoftwareUnits().get(1).getSlot());
        // 响应携带 supported models / canonicalization versions
        assertTrue(resp.getSupportedEcuSoftwareModelsCount() >= 2);
        assertTrue(resp.getSupportedInventoryCanonicalizationVersionsList().contains(2));
    }

    private DetectionCmd argumentCaptor() {
        var captor = org.mockito.ArgumentCaptor.forClass(DetectionCmd.class);
        verify(appService).detect(captor.capture());
        return captor.getValue();
    }
}
