package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
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
import static org.mockito.Mockito.*;

/**
 * 车辆清单查询服务测试（CR-019 回归 + CR-023 §2/§5.6 批量查询）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleInventoryQueryService 清单查询（CR-023 批量）")
class VehicleInventoryQueryServiceTest {

    private static final String VIN = "LSVAU2188N2ZG4G";

    @Mock private VehicleInventoryMapper vehicleInventoryMapper;
    @Mock private VehicleInventoryItemMapper vehicleInventoryItemMapper;
    @Mock private VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;

    @InjectMocks
    private VehicleInventoryQueryService vehicleInventoryQueryService;

    private VehicleInventoryPo inventory() {
        return VehicleInventoryPo.builder()
                .id(100L)
                .vin(VIN)
                .inventoryRevision(7L)
                .digest("digest-v7")
                .algorithm("SHA-256")
                .canonicalizationVersion(2)
                .canonicalDigest(new byte[]{0x0A, 0x0B})
                .acceptedTime(new Date(1762000000000L))
                .sourceCollectedAt(new Date(1761990000000L))
                .build();
    }

    private VehicleInventoryItemPo item(long id, String ecuId, String model) {
        return VehicleInventoryItemPo.builder()
                .id(id).inventoryId(100L).ecuId(ecuId)
                .hardwarePn("HW-" + ecuId)
                .hardwareVersion("H1")
                .softwareModel(model)
                .build();
    }

    private VehicleInventorySoftwareUnitPo unit(long itemId, String target, String slot, boolean active) {
        return VehicleInventorySoftwareUnitPo.builder()
                .inventoryId(100L).inventoryItemId(itemId)
                .softwareTargetCode(target)
                .softwarePartNumber("SP-" + target)
                .swVersion("1.0." + target)
                .slot(slot)
                .active(active)
                .digest("D-" + target)
                .build();
    }

    @Test
    @DisplayName("ECU→SoftwareUnit：按 inventory_id 一次批量查询并内存分组，不逐 ECU 查询")
    void getLatestByVin_batches_software_units() {
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(inventory());
        when(vehicleInventoryItemMapper.selectByInventoryId(100L)).thenReturn(List.of(
                item(10L, "ECU_A", "MULTI_TARGET"),
                item(20L, "ECU_B", "MULTI_TARGET")));
        when(vehicleInventorySoftwareUnitMapper.selectByInventoryId(100L)).thenReturn(List.of(
                unit(10L, "T1", "A", true),
                unit(10L, "T2", "B", false),
                unit(20L, "T3", "A", true)));

        InventoryProcessSummary summary = vehicleInventoryQueryService.getLatestByVin(VIN);

        assertNotNull(summary);
        assertEquals(2, summary.getEcuCount());
        assertEquals("MULTI_TARGET", summary.getInventoryModel());
        assertEquals("0a0b", summary.getCanonicalDigestHex());
        assertEquals(2, summary.getEcuList().size());
        assertEquals(2, summary.getEcuList().get(0).getSoftwareUnits().size(), "A/B Slot 必须完整保留");
        assertEquals("A", summary.getEcuList().get(0).getSoftwareUnits().get(0).getSlot());
        assertEquals("B", summary.getEcuList().get(0).getSoftwareUnits().get(1).getSlot());
        assertFalse(summary.getEcuList().get(0).getSoftwareUnits().get(1).getActive());
        // 固定批量查询次数，与 ECU 数量无关
        verify(vehicleInventorySoftwareUnitMapper, times(1)).selectByInventoryId(100L);
        verify(vehicleInventorySoftwareUnitMapper, never()).selectByInventoryItemId(anyLong());
    }

    @Test
    @DisplayName("空清单：无 ECU 时不再查询 SoftwareUnit")
    void getLatestByVin_skips_unit_query_when_no_items() {
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(inventory());
        when(vehicleInventoryItemMapper.selectByInventoryId(100L)).thenReturn(List.of());

        InventoryProcessSummary summary = vehicleInventoryQueryService.getLatestByVin(VIN);

        assertNotNull(summary);
        assertEquals(0, summary.getEcuCount());
        assertTrue(summary.getEcuList().isEmpty());
        verify(vehicleInventorySoftwareUnitMapper, never()).selectByInventoryId(anyLong());
    }

    @Test
    @DisplayName("无已接受清单：返回 null（兼容既有空语义）")
    void getLatestByVin_returns_null_when_absent() {
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(null);

        assertNull(vehicleInventoryQueryService.getLatestByVin(VIN));
        verify(vehicleInventoryItemMapper, never()).selectByInventoryId(anyLong());
    }

    @Test
    @DisplayName("导出：同样批量查询，每行一个 VIN+ECU+Target+Slot")
    void exportByVin_batches_and_flattens_units() {
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(inventory());
        when(vehicleInventoryItemMapper.selectByInventoryId(100L)).thenReturn(List.of(
                item(10L, "ECU_A", "SINGLE_IMAGE"),
                item(20L, "ECU_B", "SINGLE_IMAGE")));
        when(vehicleInventorySoftwareUnitMapper.selectByInventoryId(100L)).thenReturn(List.of(
                unit(10L, "T1", null, true),
                unit(20L, "T2", null, true)));

        List<VehicleInventoryQueryService.ExportRow> rows = vehicleInventoryQueryService.exportByVin(VIN);

        assertEquals(2, rows.size());
        assertEquals("ECU_A", rows.get(0).getEcuId());
        assertEquals("T1", rows.get(0).getSoftwareTargetCode());
        verify(vehicleInventorySoftwareUnitMapper, times(1)).selectByInventoryId(100L);
        verify(vehicleInventorySoftwareUnitMapper, never()).selectByInventoryItemId(anyLong());
    }
}
