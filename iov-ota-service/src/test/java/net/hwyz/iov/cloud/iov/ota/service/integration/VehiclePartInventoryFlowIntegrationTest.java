package net.hwyz.iov.cloud.iov.ota.service.integration;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PartAssociationSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleDeviceInventoryQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleInventoryQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartHistoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CR-023 集成测试：车辆存在性 → 清单零件富化 → VMD 零件保存/重放 → 再查询
 *
 * <p>使用真实 {@link VehicleInventoryQueryService} / {@link VehicleDeviceInventoryQueryService} /
 * {@link VehiclePartAppService} / {@link VehicleAppService}，以内存表模拟 Mapper 语义
 * （vin+pn 业务键、增量 upsert、历史留痕、按 deviceCode 分组）。覆盖 §4.3 / §5 / §6
 * 与验收 §7 的端到端回归要点。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CR-023 集成 - 关联查询与零件富化/保存")
class VehiclePartInventoryFlowIntegrationTest {

    private static final String VIN = "LSVAU2188N2ZG4G";
    private static final String UNKNOWN_VIN = "LSVAU2188N2ZG4X";
    private static final String ECU_A = "ECU_A";
    private static final String ECU_B = "ECU_B";
    private static final String PN_A = "PN-A";

    @Mock private VehicleProjectionMapper vehicleProjectionMapper;
    @Mock private VehStatusMapper vehStatusMapper;
    @Mock private VehicleInventoryMapper vehicleInventoryMapper;
    @Mock private VehicleInventoryItemMapper vehicleInventoryItemMapper;
    @Mock private VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    @Mock private VehiclePartMapper vehiclePartMapper;
    @Mock private VehiclePartHistoryMapper vehiclePartHistoryMapper;

    private VehicleAppService vehicleAppService;
    private VehicleInventoryQueryService vehicleInventoryQueryService;
    private VehicleDeviceInventoryQueryService vehicleDeviceInventoryQueryService;
    private VehiclePartAppService vehiclePartAppService;

    /** vin + pn → 零件投影 */
    private final Map<String, VehiclePartPo> parts = new LinkedHashMap<>();
    private final List<VehiclePartHistoryPo> histories = new ArrayList<>();
    private final AtomicLong partIdSeq = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        vehicleAppService = new VehicleAppService(vehStatusMapper, vehicleProjectionMapper);
        vehicleInventoryQueryService = new VehicleInventoryQueryService(
                vehicleInventoryMapper, vehicleInventoryItemMapper, vehicleInventorySoftwareUnitMapper);
        vehicleDeviceInventoryQueryService = new VehicleDeviceInventoryQueryService(
                vehicleInventoryQueryService, vehiclePartMapper);
        vehiclePartAppService = new VehiclePartAppService(vehiclePartMapper, vehiclePartHistoryMapper);

        // 车辆主档投影
        when(vehicleProjectionMapper.countByVin(VIN)).thenReturn(1);
        when(vehicleProjectionMapper.countByVin(UNKNOWN_VIN)).thenReturn(0);

        // 最新已接受清单：ECU_A/ECU_B，ECU_A 为 MULTI_TARGET + A/B Slot
        VehicleInventoryPo inventory = VehicleInventoryPo.builder()
                .id(1L).vin(VIN).inventoryRevision(3L)
                .digest("digest-3").algorithm("SHA-256")
                .canonicalizationVersion(2)
                .canonicalDigest(new byte[]{0x01, 0x02})
                .acceptedTime(new Date(1762000000000L))
                .sourceCollectedAt(new Date(1761990000000L))
                .build();
        when(vehicleInventoryMapper.selectLatestByVin(VIN)).thenReturn(inventory);
        when(vehicleInventoryItemMapper.selectByInventoryId(1L)).thenReturn(List.of(
                VehicleInventoryItemPo.builder().id(10L).inventoryId(1L).ecuId(ECU_A)
                        .hardwarePn("HW-A").hardwareVersion("H1").softwareModel("MULTI_TARGET").build(),
                VehicleInventoryItemPo.builder().id(20L).inventoryId(1L).ecuId(ECU_B)
                        .hardwarePn("HW-B").hardwareVersion("H1").softwareModel("SINGLE_IMAGE").build()));
        when(vehicleInventorySoftwareUnitMapper.selectByInventoryId(1L)).thenReturn(List.of(
                VehicleInventorySoftwareUnitPo.builder().inventoryId(1L).inventoryItemId(10L)
                        .softwareTargetCode("T1").softwarePartNumber("SP1").swVersion("1.0")
                        .slot("A").active(true).digest("D1").build(),
                VehicleInventorySoftwareUnitPo.builder().inventoryId(1L).inventoryItemId(10L)
                        .softwareTargetCode("T2").softwarePartNumber("SP2").swVersion("2.0")
                        .slot("B").active(false).digest("D2").build(),
                VehicleInventorySoftwareUnitPo.builder().inventoryId(1L).inventoryItemId(20L)
                        .softwareTargetCode("T3").softwarePartNumber("SP3").swVersion("3.0")
                        .slot("A").active(true).digest("D3").build()));

        // 零件投影内存表
        when(vehiclePartMapper.selectPoByVinAndPn(anyString(), anyString()))
                .thenAnswer(inv -> parts.get(inv.getArgument(0) + "|" + inv.getArgument(1)));
        when(vehiclePartMapper.selectByVin(anyString())).thenAnswer(inv ->
                parts.values().stream().filter(p -> inv.getArgument(0).equals(p.getVin())).toList());
        when(vehiclePartMapper.insertPo(any())).thenAnswer(inv -> {
            VehiclePartPo po = inv.getArgument(0);
            po.setId(partIdSeq.incrementAndGet());
            parts.put(po.getVin() + "|" + po.getPn(), po);
            return 1;
        });
        when(vehiclePartMapper.updateManagedFields(any())).thenAnswer(inv -> {
            VehiclePartPo po = inv.getArgument(0);
            parts.put(po.getVin() + "|" + po.getPn(), po);
            return 1;
        });
        when(vehiclePartHistoryMapper.insertPo(any())).thenAnswer(inv -> {
            histories.add(inv.getArgument(0));
            return 1;
        });
    }

    private VehiclePartPo incomingPart(String pn, String deviceCode, String configWord) {
        return VehiclePartPo.builder()
                .vin(VIN).pn(pn).deviceCode(deviceCode)
                .deviceItem("ITEM-" + pn).sn("SN-" + pn)
                .configWord(configWord).supplierCode("SUP").batchNum("B1")
                .hardwareVer("HW1").softwareVer("SW1")
                .hardwarePn("HPN").softwarePn("SPN").extra("{}")
                .build();
    }

    @Test
    @DisplayName("车辆不存在：明确错误，不查询清单/零件")
    void unknown_vehicle_returns_explicit_error() {
        assertThrows(VehicleNotExistException.class, () -> vehicleAppService.requireVehicleExists(UNKNOWN_VIN));

        verify(vehicleInventoryMapper, never()).selectLatestByVin(anyString());
        verify(vehiclePartMapper, never()).selectByVin(anyString());
    }

    @Test
    @DisplayName("清单零件富化 → 保存 → 再查询：配置字可刷新，历史留痕，重放幂等")
    void save_parts_then_inventory_reflects_changes() {
        vehicleAppService.requireVehicleExists(VIN);

        // 1. 初始无零件：两个 ECU 均 NOT_FOUND
        InventoryProcessSummary initial = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);
        assertNotNull(initial);
        assertEquals(2, initial.getEcuCount());
        assertEquals(2, initial.getEcuList().get(0).getSoftwareUnits().size(), "A/B Slot 完整展示");
        assertEquals(PartAssociationSummary.STATUS_NOT_FOUND,
                initial.getEcuList().get(0).getPartAssociation().getStatus());

        // 2. VMD 首次同步 ECU_A 零件
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incomingPart(PN_A, ECU_A, "CFG-OLD")));
        assertEquals(1, histories.size());
        assertEquals("VMD新增", histories.get(0).getDescription());

        InventoryProcessSummary afterInsert = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);
        PartAssociationSummary matched = afterInsert.getEcuList().get(0).getPartAssociation();
        assertEquals(PartAssociationSummary.STATUS_MATCHED, matched.getStatus());
        assertEquals(1, matched.getCandidateCount());
        assertEquals(PN_A, matched.getPart().getPn());
        assertEquals("CFG-OLD", matched.getPart().getConfigWord());
        assertEquals(PartAssociationSummary.STATUS_NOT_FOUND,
                afterInsert.getEcuList().get(1).getPartAssociation().getStatus());

        // 3. 无变化重放：不产生新历史、不更新
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incomingPart(PN_A, ECU_A, "CFG-OLD")));
        assertEquals(1, histories.size(), "无变化重放必须幂等");
        verify(vehiclePartMapper, never()).updateManagedFields(any());

        // 4. 配置字变化（非 SN）：必须刷新投影并写 CHANGE 历史
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incomingPart(PN_A, ECU_A, "CFG-NEW")));
        assertEquals(2, histories.size());
        assertTrue(histories.get(1).getDescription().contains("configWord"));
        assertEquals("CFG-NEW", histories.get(1).getConfigWord());
        verify(vehiclePartMapper, times(1)).updateManagedFields(any());

        InventoryProcessSummary afterUpdate = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);
        assertEquals("CFG-NEW", afterUpdate.getEcuList().get(0).getPartAssociation().getPart().getConfigWord());
        // 每次清单查询只做一次零件批量查询（2 次查询共 3 次调用：初始/插入后/更新后）
        verify(vehiclePartMapper, times(3)).selectByVin(VIN);
        verify(vehicleInventorySoftwareUnitMapper, times(3)).selectByInventoryId(1L);
    }

    @Test
    @DisplayName("同 ECU 多零件：AMBIGUOUS，不任取候选配置字")
    void multiple_parts_for_same_ecu_is_ambiguous() {
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(
                incomingPart("PN-1", ECU_A, "CFG-1"),
                incomingPart("PN-2", ECU_A, "CFG-2")));

        InventoryProcessSummary summary = vehicleDeviceInventoryQueryService.getLatestByVin(VIN);
        PartAssociationSummary association = summary.getEcuList().get(0).getPartAssociation();

        assertEquals(PartAssociationSummary.STATUS_AMBIGUOUS, association.getStatus());
        assertEquals(2, association.getCandidateCount());
        assertNull(association.getPart());
        verify(vehiclePartMapper, times(1)).selectByVin(VIN);
    }

    @Test
    @DisplayName("增量请求：只处理出现的 vin+pn，不删除其他零件")
    void incremental_request_never_deletes_other_parts() {
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(
                incomingPart("PN-1", ECU_A, "CFG-1"),
                incomingPart("PN-2", ECU_B, "CFG-2")));
        vehiclePartAppService.saveVehicleParts(VIN, "VMD", List.of(incomingPart("PN-1", ECU_A, "CFG-1X")));

        assertEquals(2, parts.size(), "未出现在请求中的零件必须保持");
        verify(vehiclePartMapper, never()).physicalDeletePo(any());
        verify(vehiclePartMapper, never()).logicalDeletePo(any());
        verify(vehiclePartMapper, never()).batchPhysicalDeletePo(any());
        verify(vehicleInventorySoftwareUnitMapper, never()).selectByInventoryItemId(anyLong());
    }
}
