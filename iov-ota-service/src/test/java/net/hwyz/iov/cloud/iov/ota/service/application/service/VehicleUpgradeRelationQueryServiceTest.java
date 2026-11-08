package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.VehicleRelationTaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleUpgradeRelationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 车辆升级关联查询服务测试（CR-023 §4）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleUpgradeRelationQueryService 关联查询")
class VehicleUpgradeRelationQueryServiceTest {

    private static final String VIN = "LSVAU2188N2ZG4G";

    @Mock private VehicleUpgradeRelationMapper vehicleUpgradeRelationMapper;

    @InjectMocks
    private VehicleUpgradeRelationQueryService vehicleUpgradeRelationQueryService;

    @Test
    @DisplayName("活动：一次关联查询返回去重活动，不逐行补全")
    void listActivities_single_relation_query() {
        List<VehicleActivityRelationResult> rows = List.of(
                VehicleActivityRelationResult.builder().activityId(1L).vehicleTaskCount(2L).build(),
                VehicleActivityRelationResult.builder().activityId(2L).vehicleTaskCount(1L).build());
        when(vehicleUpgradeRelationMapper.selectActivityRelations(VIN)).thenReturn(rows);

        List<VehicleActivityRelationResult> result = vehicleUpgradeRelationQueryService.listActivities(VIN);

        assertEquals(2, result.size());
        verify(vehicleUpgradeRelationMapper, times(1)).selectActivityRelations(VIN);
        verifyNoMoreInteractions(vehicleUpgradeRelationMapper);
    }

    @Test
    @DisplayName("任务：过滤条件原样下推 SQL，返回关联结果")
    void listTasks_passes_filters_to_sql() {
        Date begin = new Date(1761900000000L);
        Date end = new Date(1762000000000L);
        VehicleRelationTaskQuery query = VehicleRelationTaskQuery.builder()
                .vin(VIN).activityId(11L).taskState(8)
                .vehicleTaskStatus("SUCCEEDED")
                .beginTime(begin).endTime(end)
                .build();
        List<VehicleTaskRelationResult> rows = List.of(
                VehicleTaskRelationResult.builder().vehicleTaskId(1L).taskId(21L).build());
        when(vehicleUpgradeRelationMapper.selectTaskRelations(any())).thenReturn(rows);

        List<VehicleTaskRelationResult> result = vehicleUpgradeRelationQueryService.listTasks(query);

        assertEquals(1, result.size());
        ArgumentCaptor<VehicleRelationTaskQuery> captor = ArgumentCaptor.forClass(VehicleRelationTaskQuery.class);
        verify(vehicleUpgradeRelationMapper, times(1)).selectTaskRelations(captor.capture());
        assertEquals(VIN, captor.getValue().getVin());
        assertEquals(11L, captor.getValue().getActivityId());
        assertEquals(8, captor.getValue().getTaskState());
        assertEquals("SUCCEEDED", captor.getValue().getVehicleTaskStatus());
        assertEquals(begin, captor.getValue().getBeginTime());
        assertEquals(end, captor.getValue().getEndTime());
    }

    @Test
    @DisplayName("空分页：无关联数据返回空列表")
    void empty_relations() {
        when(vehicleUpgradeRelationMapper.selectActivityRelations(VIN)).thenReturn(List.of());
        when(vehicleUpgradeRelationMapper.selectTaskRelations(any())).thenReturn(List.of());

        assertTrue(vehicleUpgradeRelationQueryService.listActivities(VIN).isEmpty());
        assertTrue(vehicleUpgradeRelationQueryService.listTasks(
                VehicleRelationTaskQuery.builder().vin(VIN).build()).isEmpty());
    }
}
