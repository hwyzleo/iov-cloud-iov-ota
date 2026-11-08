package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 升级车辆管理 VO 组装测试（CR-022 §5.2）
 *
 * @author hwyz_leo
 */
@DisplayName("VehicleProjectionMptAssembler 管理查询组装（CR-022）")
class VehicleProjectionMptAssemblerTest {

    private final VehicleProjectionMptAssembler assembler = VehicleProjectionMptAssembler.INSTANCE;

    @Test
    @DisplayName("fromResult：主档字段与 lastInventoryReportedAt 一并返回")
    void fromResult_maps_reported_at() {
        Date reportedAt = new Date(1762000000000L);
        VehicleManagementResult result = VehicleManagementResult.builder()
                .id(7L).vin("LSVAU2188N2ZG4G")
                .plantCode("P1").configurationCode("CFG1").sourceVersion(3L)
                .lastInventoryReportedAt(reportedAt)
                .build();

        VehicleMpt vo = assembler.fromResult(result);

        assertEquals(7L, vo.getId());
        assertEquals("LSVAU2188N2ZG4G", vo.getVin());
        assertEquals("CFG1", vo.getConfigurationCode());
        assertEquals(reportedAt, vo.getLastInventoryReportedAt());
    }

    @Test
    @DisplayName("fromResult：无运行态记录时 lastInventoryReportedAt 为 null")
    void fromResult_null_reported_at() {
        VehicleMpt vo = assembler.fromResult(VehicleManagementResult.builder().id(1L).build());
        assertNull(vo.getLastInventoryReportedAt());
    }

    @Test
    @DisplayName("fromResultList：批量映射保持顺序与字段")
    void fromResultList_maps_all() {
        Date a = new Date(1762000000000L);
        Date b = new Date(1762000001000L);
        List<VehicleMpt> list = assembler.fromResultList(List.of(
                VehicleManagementResult.builder().id(1L).vin("VIN1").lastInventoryReportedAt(a).build(),
                VehicleManagementResult.builder().id(2L).vin("VIN2").lastInventoryReportedAt(b).build()));

        assertEquals(2, list.size());
        assertEquals(a, list.get(0).getLastInventoryReportedAt());
        assertEquals(b, list.get(1).getLastInventoryReportedAt());
    }

    @Test
    @DisplayName("不变量：车辆主档 PO 不持有 OTA 运行态字段，杜绝回写主档投影")
    void projection_po_has_no_report_state_field() {
        assertTrue(fieldNames(VehicleProjectionPo.class).stream()
                        .noneMatch(n -> n.equals("lastInventoryReportedAt")),
                "tb_vehicle_projection 映射不得包含 lastInventoryReportedAt");
    }

    private static List<String> fieldNames(Class<?> type) {
        List<String> names = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields()).map(Field::getName).forEach(names::add);
        }
        return names;
    }
}
