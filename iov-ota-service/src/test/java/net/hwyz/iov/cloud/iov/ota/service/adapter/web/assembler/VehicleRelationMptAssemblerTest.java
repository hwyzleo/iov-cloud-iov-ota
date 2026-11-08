package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleActivityRelationMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleTaskRelationMpt;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆关联查询结果装配测试（CR-023 §4）
 *
 * @author hwyz_leo
 */
@DisplayName("VehicleRelationMptAssembler 状态码转枚举名")
class VehicleRelationMptAssemblerTest {

    @Test
    @DisplayName("活动：状态码转 ActivityState 枚举名，聚合字段透传")
    void maps_activity_relation() {
        Date latest = new Date(1762000000000L);
        VehicleActivityRelationResult result = VehicleActivityRelationResult.builder()
                .activityId(11L).activityName("活动A").activityState(5)
                .vehicleTaskCount(3L).latestTaskTime(latest)
                .build();

        VehicleActivityRelationMpt vo = VehicleRelationMptAssembler.toActivityVo(result);

        assertEquals(11L, vo.getActivityId());
        assertEquals("活动A", vo.getActivityName());
        assertEquals("RELEASED", vo.getActivityState());
        assertEquals(3L, vo.getVehicleTaskCount());
        assertEquals(latest, vo.getLatestTaskTime());
    }

    @Test
    @DisplayName("任务：phase/state/activityState 均转枚举名，窗口字段不互换")
    void maps_task_relation() {
        Date releaseAt = new Date(1761900000000L);
        Date start = new Date(1761910000000L);
        Date end = new Date(1761920000000L);
        Date create = new Date(1761930000000L);
        VehicleTaskRelationResult result = VehicleTaskRelationResult.builder()
                .vehicleTaskId(501L).vehicleTaskStatus("SUCCEEDED").resultCode("OK")
                .taskId(21L).taskName("任务A").taskPhase(2).taskState(8)
                .activityId(11L).activityName("活动A").activityState(6)
                .releaseAt(releaseAt).startTime(start).endTime(end).createTime(create)
                .build();

        VehicleTaskRelationMpt vo = VehicleRelationMptAssembler.toTaskVo(result);

        assertEquals(501L, vo.getVehicleTaskId());
        assertEquals("SUCCEEDED", vo.getVehicleTaskStatus());
        assertEquals("OK", vo.getResultCode());
        assertEquals("任务A", vo.getTaskName());
        assertEquals("CANARY", vo.getTaskPhase());
        assertEquals("COMPLETED", vo.getTaskState());
        assertEquals("FINISHED", vo.getActivityState());
        assertEquals(releaseAt, vo.getReleaseAt());
        assertEquals(start, vo.getStartTime());
        assertEquals(end, vo.getEndTime());
        assertEquals(create, vo.getCreateTime());
    }

    @Test
    @DisplayName("空值与未知状态码：不抛异常，返回 null")
    void handles_null_and_unknown_codes() {
        VehicleActivityRelationResult unknown = VehicleActivityRelationResult.builder()
                .activityId(1L).activityState(99).build();
        assertNull(VehicleRelationMptAssembler.toActivityVo(unknown).getActivityState());
        assertNull(VehicleRelationMptAssembler.toActivityVo(null));

        VehicleTaskRelationResult nullCodes = VehicleTaskRelationResult.builder()
                .vehicleTaskId(2L).build();
        VehicleTaskRelationMpt vo = VehicleRelationMptAssembler.toTaskVo(nullCodes);
        assertNull(vo.getTaskPhase());
        assertNull(vo.getTaskState());
        assertNull(vo.getActivityState());
        assertNull(VehicleRelationMptAssembler.toTaskVo(null));
    }

    @Test
    @DisplayName("列表装配：null 输入返回空列表")
    void null_lists_return_empty() {
        assertTrue(VehicleRelationMptAssembler.toActivityVoList(null).isEmpty());
        assertTrue(VehicleRelationMptAssembler.toTaskVoList(null).isEmpty());
        assertEquals(1, VehicleRelationMptAssembler.toTaskVoList(List.of(
                VehicleTaskRelationResult.builder().vehicleTaskId(1L).build())).size());
    }
}
