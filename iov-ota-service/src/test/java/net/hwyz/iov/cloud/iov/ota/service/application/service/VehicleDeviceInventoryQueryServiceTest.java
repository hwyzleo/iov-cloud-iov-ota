package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PartAssociationSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehiclePartSummary;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
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
import static org.mockito.Mockito.*;

/**
 * 车辆设备软件清单富化服务测试（CR-023 §4.3 / §5）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleDeviceInventoryQueryService 零件关联富化（CR-023）")
class VehicleDeviceInventoryQueryServiceTest {

    private static final String VIN = "LSVAU2188N2ZG4G";

    @Mock private VehicleInventoryQueryService vehicleInventoryQueryService;
    @Mock private VehiclePartMapper vehiclePartMapper;

    @InjectMocks
    private VehicleDeviceInventoryQueryService vehicleDeviceInventoryQueryService;

    private InventoryProcessSummary summary(String... ecuIds) {
        List<InventoryProcessSummary.EcuInventorySummary> ecus = java.util.Arrays.stream(ecuIds)
                .map(ecuId -> InventoryProcessSummary.EcuInventorySummary.builder()
                        .ecuId(ecuId)
                        .hardwarePn("HW-" + ecuId)
                        .hardwareVersion("H1")
                        .softwareModel("SINGLE_IMAGE")
                        .softwareUnits(List.of(InventoryProcessSummary.SoftwareUnitSummary.builder()
                                .softwareTargetCode("T-" + ecuId)
                                .softwarePartNumber("SP-" + ecuId)
                                .swVersion("1.0")
                                .slot("A")
                                .active(true)
                                .digest("D1")
                                .build()))
                        .build())
                .toList();
        return InventoryProcessSummary.builder()
                .inventoryRevision(9L)
                .ecuCount(ecus.size())
                .inventoryModel("MULTI_TARGET")
                .canonicalDigestHex("0a0b")
                .acceptedTime(java.time.Instant.ofEpochMilli(1762000000000L))
                .ecuList(ecus)
                .build();
    }

    private VehiclePartPo part(long id, String pn, String deviceCode, String configWord) {
        return VehiclePartPo.builder()
                .id(id).vin(VIN).pn(pn).deviceCode(deviceCode)
                .deviceItem("ITEM-" + pn).sn("SN-" + id)
                .configWord(configWord)
                .supplierCode("SUP").batchNum("B1")
                .hardwareVer("HW1").softwareVer("SW1")
                .hardwarePn("HPN").softwarePn("SPN")
                .extra("{\"k\":\"v\"}")
                .modifyTime(new Date(1762000000000L))
                .build();
    }

    @Test
    @DisplayName("唯一候选：MATCHED，返回零件全字段与原始配置字")
    void matched_returns_full_part_with_raw_config_word() {
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(summary("ECU_A", "ECU_B"));
        when(vehiclePartMapper.selectByVin(VIN)).thenReturn(List.of(part(1L, "PN_A", "ECU_A", "CFG-0x1122")));

        InventoryProcessSummary result = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);

        PartAssociationSummary association = result.getEcuList().get(0).getPartAssociation();
        assertEquals(PartAssociationSummary.STATUS_MATCHED, association.getStatus());
        assertEquals(1, association.getCandidateCount());
        VehiclePartSummary part = association.getPart();
        assertEquals("PN_A", part.getPn());
        assertEquals("ECU_A", part.getDeviceCode());
        assertEquals("CFG-0x1122", part.getConfigWord(), "配置字必须原样返回");
        assertEquals("{\"k\":\"v\"}", part.getExtra());
        assertNotNull(part.getModifyTime());
        // 无候选的 ECU 仍返回显式状态
        assertEquals(PartAssociationSummary.STATUS_NOT_FOUND,
                result.getEcuList().get(1).getPartAssociation().getStatus());
        assertEquals(0, result.getEcuList().get(1).getPartAssociation().getCandidateCount());
        assertNull(result.getEcuList().get(1).getPartAssociation().getPart());
        // 一次批量查询，与 ECU 数量无关
        verify(vehiclePartMapper, times(1)).selectByVin(VIN);
    }

    @Test
    @DisplayName("多候选：AMBIGUOUS，不任取一条、part 为 null")
    void ambiguous_does_not_pick_any_candidate() {
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(summary("ECU_A"));
        when(vehiclePartMapper.selectByVin(VIN)).thenReturn(List.of(
                part(1L, "PN_1", "ECU_A", "CFG-1"),
                part(2L, "PN_2", "ECU_A", "CFG-2")));

        InventoryProcessSummary result = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);

        PartAssociationSummary association = result.getEcuList().get(0).getPartAssociation();
        assertEquals(PartAssociationSummary.STATUS_AMBIGUOUS, association.getStatus());
        assertEquals(2, association.getCandidateCount());
        assertNull(association.getPart(), "AMBIGUOUS 不得用 LIMIT 1 / 最新记录填充");
    }

    @Test
    @DisplayName("精确匹配：大小写不同不匹配，不做 PN/版本模糊兜底")
    void never_falls_back_to_hardware_pn_or_case_insensitive_match() {
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(summary("ECU_A"));
        VehiclePartPo wrongCase = part(1L, "PN_A", "ecu_a", "CFG-1");
        wrongCase.setHardwarePn("HW-ECU_A");
        when(vehiclePartMapper.selectByVin(VIN)).thenReturn(List.of(wrongCase));

        InventoryProcessSummary result = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);

        assertEquals(PartAssociationSummary.STATUS_NOT_FOUND,
                result.getEcuList().get(0).getPartAssociation().getStatus());
        assertNull(result.getEcuList().get(0).getPartAssociation().getPart());
    }

    @Test
    @DisplayName("无已接受清单：返回 null 且不查询零件，不以 VMD 零件伪造清单")
    void no_inventory_never_queries_parts() {
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(null);

        assertNull(vehicleDeviceInventoryQueryService.getLatestByVin(VIN));

        verify(vehiclePartMapper, never()).selectByVin(any());
    }

    @Test
    @DisplayName("空 ECU 清单：保留既有空语义，不查询零件")
    void empty_ecu_list_skips_part_query() {
        InventoryProcessSummary empty = InventoryProcessSummary.builder()
                .inventoryRevision(1L).ecuCount(0).ecuList(List.of()).build();
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(empty);

        InventoryProcessSummary result = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);

        assertSame(empty, result);
        verify(vehiclePartMapper, never()).selectByVin(any());
    }

    @Test
    @DisplayName("MULTI_TARGET / A-B Slot：SoftwareUnit 完整保留，仅追加 partAssociation")
    void multi_target_and_slots_are_preserved() {
        InventoryProcessSummary multi = InventoryProcessSummary.builder()
                .inventoryRevision(2L)
                .ecuCount(1)
                .inventoryModel("MULTI_TARGET")
                .ecuList(List.of(InventoryProcessSummary.EcuInventorySummary.builder()
                        .ecuId("ECU_A")
                        .hardwarePn("HW-ECU_A")
                        .softwareModel("MULTI_TARGET")
                        .softwareUnits(List.of(
                                InventoryProcessSummary.SoftwareUnitSummary.builder()
                                        .softwareTargetCode("T1").slot("A").active(true).swVersion("1.0").build(),
                                InventoryProcessSummary.SoftwareUnitSummary.builder()
                                        .softwareTargetCode("T2").slot("B").active(false).swVersion("2.0").build()))
                        .build()))
                .build();
        when(vehicleInventoryQueryService.getLatestByVin(VIN)).thenReturn(multi);
        when(vehiclePartMapper.selectByVin(VIN)).thenReturn(List.of(part(1L, "PN_A", "ECU_A", "CFG-A")));

        InventoryProcessSummary result = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);

        assertEquals(2, result.getEcuList().get(0).getSoftwareUnits().size());
        assertEquals("A", result.getEcuList().get(0).getSoftwareUnits().get(0).getSlot());
        assertEquals("B", result.getEcuList().get(0).getSoftwareUnits().get(1).getSlot());
        assertEquals(PartAssociationSummary.STATUS_MATCHED,
                result.getEcuList().get(0).getPartAssociation().getStatus());
    }
}
