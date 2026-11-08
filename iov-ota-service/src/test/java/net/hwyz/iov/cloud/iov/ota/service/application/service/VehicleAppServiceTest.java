package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 升级车辆管理查询服务测试（CR-022 §5）
 *
 * <p>列表/详情必须一次关联查询返回运行态最后上报时间，禁止逐 VIN 查询（NZ-022-5）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleAppService 升级车辆管理查询（CR-022）")
class VehicleAppServiceTest {

    private static final String VIN = "LSVAU2188N2ZG4G";

    @Mock private VehStatusMapper vehStatusMapper;
    @Mock private VehicleProjectionMapper vehicleProjectionMapper;

    @InjectMocks
    private VehicleAppService vehicleAppService;

    @Test
    @DisplayName("列表：一次关联查询透传运行态最后上报时间，无逐 VIN 查询")
    void search_returns_joined_results_without_n_plus_one() {
        Date reportedAt = new Date(1762000000000L);
        VehicleManagementResult row = VehicleManagementResult.builder()
                .id(1L).vin(VIN).configurationCode("CFG1")
                .lastInventoryReportedAt(reportedAt)
                .build();
        when(vehicleProjectionMapper.selectManagementList(any(), any(), any()))
                .thenReturn(List.of(row));

        List<VehicleManagementResult> results =
                vehicleAppService.search(VIN, null, null);

        assertEquals(1, results.size());
        assertEquals(reportedAt, results.get(0).getLastInventoryReportedAt());
        verify(vehicleProjectionMapper, times(1)).selectManagementList(VIN, null, null);
        // 禁止逐 VIN 查询运行态或主档
        verify(vehicleProjectionMapper, never()).selectByVin(anyString());
        verify(vehicleProjectionMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("详情：运行态无记录时 lastInventoryReportedAt 为 null")
    void getVehicleByVin_returns_null_reported_at_when_absent() {
        VehicleManagementResult row = VehicleManagementResult.builder().id(1L).vin(VIN).build();
        when(vehicleProjectionMapper.selectManagementByVin(VIN)).thenReturn(row);

        VehicleManagementResult result = vehicleAppService.getVehicleByVin(VIN);

        assertNotNull(result);
        assertNull(result.getLastInventoryReportedAt());
        verify(vehicleProjectionMapper, times(1)).selectManagementByVin(VIN);
    }

    @Test
    @DisplayName("详情：车辆主档不存在时返回 null")
    void getVehicleByVin_returns_null_when_projection_absent() {
        when(vehicleProjectionMapper.selectManagementByVin(VIN)).thenReturn(null);

        assertNull(vehicleAppService.getVehicleByVin(VIN));
        verify(vehicleProjectionMapper, times(1)).selectManagementByVin(VIN);
    }
}
