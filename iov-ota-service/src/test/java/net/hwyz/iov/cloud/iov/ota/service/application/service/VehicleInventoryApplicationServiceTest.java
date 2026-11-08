package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.SoftwareUnitCmd;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryReportStateMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 车辆清单应用服务测试（CR-019 §6.1/§6.2）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleInventoryApplicationService FULL/DIGEST（CR-019）")
class VehicleInventoryApplicationServiceTest {

    @Mock private VehicleInventoryMapper vehicleInventoryMapper;
    @Mock private VehicleInventoryItemMapper vehicleInventoryItemMapper;
    @Mock private VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    @Mock private VehicleInventoryReportStateMapper vehicleInventoryReportStateMapper;
    @Mock private InventoryObservedOutboxProducer observedOutboxProducer;

    @InjectMocks
    private VehicleInventoryApplicationService service;

    private static final String VIN = "LSVAU2188N2ZG4G";

    @BeforeEach
    void setUp() {
        when(observedOutboxProducer.produce(any(), any(), any(), any())).thenReturn(true);
    }

    private DetectionCmd fullLegacyCmd() {
        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(VIN);
        cmd.setInventoryMode("FULL");
        cmd.setInventoryRevision(1L);
        cmd.setCanonicalizationVersion(1);
        cmd.setCollectedAt(1700000000000L);
        cmd.setInventoryItems(List.of(InventoryItemCmd.builder()
                .ecuId("ECU1")
                .softwarePn("SPN1").softwareVersion("V1.0")
                .hardwarePn("HPN1").hardwareVersion("H1")
                .build()));
        return cmd;
    }

    private DetectionCmd fullMultiTargetCmd() {
        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(VIN);
        cmd.setInventoryMode("FULL");
        cmd.setInventoryRevision(1L);
        cmd.setCanonicalizationVersion(2);
        cmd.setCollectedAt(1700000000000L);
        cmd.setInventoryItems(List.of(InventoryItemCmd.builder()
                .ecuId("ECU1").softwareModel("MULTI_TARGET")
                .hardwarePn("HPN1").hardwareVersion("H1")
                .softwareUnits(List.of(
                        SoftwareUnitCmd.builder()
                                .softwareTargetCode("BOOT").softwarePartNumber("SPN-B").swVersion("V1.0")
                                .active(true).build(),
                        SoftwareUnitCmd.builder()
                                .softwareTargetCode("APP").softwarePartNumber("SPN-A").swVersion("V2.0")
                                .slot("A").active(true).build(),
                        SoftwareUnitCmd.builder()
                                .softwareTargetCode("APP").softwarePartNumber("SPN-A").swVersion("V2.0")
                                .slot("B").active(false).build()))
                .build()));
        return cmd;
    }

    @Test
    @DisplayName("FULL legacy 受理：写 header + item + ECU_IMAGE unit + 观测 Outbox + 运行态最后上报时间")
    void full_legacy_accepts_and_persists() {
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(null);
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(fullLegacyCmd());

        assertEquals(InventoryDisposition.ACCEPTED, result.getDisposition());
        assertEquals("SINGLE_IMAGE", result.getInventoryModel());
        assertEquals(1, result.getCanonicalizationVersion());
        verify(vehicleInventoryMapper, times(1)).insert(any(VehicleInventoryPo.class));
        verify(vehicleInventoryItemMapper, times(1)).insert(any(VehicleInventoryItemPo.class));
        verify(vehicleInventorySoftwareUnitMapper, times(1)).insert(any());
        verify(observedOutboxProducer, times(1)).produce(any(), any(), any(), any());
        // CR-022：首次有效 FULL 建立运行态最后上报时间
        verify(vehicleInventoryReportStateMapper, times(1)).upsertReported(eq(VIN), any(Date.class));
    }

    @Test
    @DisplayName("FULL MULTI_TARGET 受理：每个 unit 写一行子表")
    void full_multi_target_persists_units() {
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(null);
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(fullMultiTargetCmd());

        assertEquals(InventoryDisposition.ACCEPTED, result.getDisposition());
        assertEquals("MULTI_TARGET", result.getInventoryModel());
        assertEquals(2, result.getCanonicalizationVersion());
        assertNotNull(result.getCanonicalDigestHex());
        verify(vehicleInventorySoftwareUnitMapper, times(3)).insert(any());
        verify(observedOutboxProducer, times(1)).produce(any(), any(), any(), any());
    }

    @Test
    @DisplayName("FULL 校验失败（模式冲突）→ FULL_REQUIRED，无持久化，不刷新运行态")
    void full_validation_failure_no_persist() {
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(null);
        DetectionCmd cmd = fullLegacyCmd();
        cmd.getInventoryItems().get(0).setSoftwareModel("MULTI_TARGET");
        cmd.getInventoryItems().get(0).setSoftwareUnits(List.of(
                SoftwareUnitCmd.builder().softwareTargetCode("APP")
                        .softwarePartNumber("SPN-A").swVersion("V2.0").build()));
        cmd.getInventoryItems().get(0).setSoftwarePn("SPN1");

        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.FULL_REQUIRED, result.getDisposition());
        verify(vehicleInventoryMapper, never()).insert(any());
        verify(observedOutboxProducer, never()).produce(any(), any(), any(), any());
        verify(vehicleInventoryReportStateMapper, never()).upsertReported(any(), any());
    }

    @Test
    @DisplayName("FULL 同 revision 不同摘要 → REVISION_CONFLICT")
    void full_revision_conflict() {
        VehicleInventoryPo existing = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(1L)
                .digest("old-digest").algorithm("SHA-256")
                .canonicalizationVersion(1)
                .acceptedTime(new Date())
                .build();
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(existing);

        DetectionCmd cmd = fullLegacyCmd();
        cmd.setInventoryDigest("new-digest");
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.REVISION_CONFLICT, result.getDisposition());
        verify(vehicleInventoryMapper, never()).insert(any());
        // CR-022：冲突拒绝不刷新运行态
        verify(vehicleInventoryReportStateMapper, never()).upsertReported(any(), any());
    }

    @Test
    @DisplayName("FULL 幂等重放（同 revision 同摘要）→ ACCEPTED 复用，不发新事件，刷新运行态")
    void full_idempotent_replay() {
        VehicleInventoryPo existing = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(1L)
                .digest("abc123").algorithm("SHA-256")
                .canonicalizationVersion(1)
                .acceptedTime(new Date())
                .build();
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(existing);

        DetectionCmd cmd = fullLegacyCmd();
        cmd.setInventoryDigest("abc123");
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.ACCEPTED, result.getDisposition());
        verify(vehicleInventoryMapper, never()).insert(any());
        verify(observedOutboxProducer, never()).produce(any(), any(), any(), any());
        // CR-022：新消息身份的 FULL 业务幂等重放仍刷新最后上报时间（accepted_time 不变）
        verify(vehicleInventoryReportStateMapper, times(1)).upsertReported(eq(VIN), any(Date.class));
    }

    @Test
    @DisplayName("运行态 upsert 异常向上抛出（随业务事务回滚，不吞异常）")
    void report_state_failure_rolls_back_business() {
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(null);
        when(vehicleInventoryReportStateMapper.upsertReported(eq(VIN), any(Date.class)))
                .thenThrow(new IllegalStateException("report state write failed"));

        assertThrows(IllegalStateException.class, () -> service.handleInventory(fullLegacyCmd()));
        // 运行态失败后不得继续创建观测 Outbox
        verify(observedOutboxProducer, never()).produce(any(), any(), any(), any());
    }

    @Test
    @DisplayName("FULL 算法不支持 → ALGORITHM_UNSUPPORTED，不刷新运行态")
    void full_algorithm_unsupported_no_refresh() {
        VehicleInventoryPo existing = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(1L)
                .digest("abc").algorithm("SHA-256")
                .canonicalizationVersion(2)
                .acceptedTime(new Date())
                .build();
        when(vehicleInventoryMapper.selectByVinAndRevision(eq(VIN), anyLong())).thenReturn(existing);

        DetectionCmd cmd = fullLegacyCmd();
        cmd.setCanonicalizationVersion(1);
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.ALGORITHM_UNSUPPORTED, result.getDisposition());
        verify(vehicleInventoryReportStateMapper, never()).upsertReported(any(), any());
    }

    @Test
    @DisplayName("DIGEST 命中 v1 → ACCEPTED 复用，不更新 accepted_time，刷新运行态")
    void digest_hit_v1() {
        VehicleInventoryPo existing = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(3L)
                .digest("abc123").algorithm("SHA-256")
                .canonicalizationVersion(1)
                .acceptedTime(new Date())
                .build();
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(existing);

        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(VIN);
        cmd.setInventoryMode("DIGEST");
        cmd.setInventoryRevision(3L);
        cmd.setCanonicalizationVersion(1);
        cmd.setInventoryDigest("abc123");
        cmd.setDigestAlgorithm("SHA-256");
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.ACCEPTED, result.getDisposition());
        verify(observedOutboxProducer, never()).produce(any(), any(), any(), any());
        // CR-022：DIGEST 命中仍刷新最后上报时间
        verify(vehicleInventoryReportStateMapper, times(1)).upsertReported(eq(VIN), any(Date.class));
    }

    @Test
    @DisplayName("DIGEST 摘要不匹配 → DIGEST_MISMATCH")
    void digest_mismatch() {
        VehicleInventoryPo existing = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(3L)
                .digest("abc123").algorithm("SHA-256")
                .canonicalizationVersion(1)
                .acceptedTime(new Date())
                .build();
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(existing);

        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(VIN);
        cmd.setInventoryMode("DIGEST");
        cmd.setInventoryRevision(3L);
        cmd.setCanonicalizationVersion(1);
        cmd.setInventoryDigest("different");
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.DIGEST_MISMATCH, result.getDisposition());
        verify(vehicleInventoryReportStateMapper, never()).upsertReported(any(), any());
    }

    @Test
    @DisplayName("DIGEST 无存量 FULL → FULL_REQUIRED")
    void digest_no_full() {
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(null);
        DetectionCmd cmd = new DetectionCmd();
        cmd.setVin(VIN);
        cmd.setInventoryMode("DIGEST");
        cmd.setInventoryRevision(1L);
        cmd.setCanonicalizationVersion(2);
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                service.handleInventory(cmd);
        assertEquals(InventoryDisposition.FULL_REQUIRED, result.getDisposition());
        verify(vehicleInventoryReportStateMapper, never()).upsertReported(any(), any());
    }
}
