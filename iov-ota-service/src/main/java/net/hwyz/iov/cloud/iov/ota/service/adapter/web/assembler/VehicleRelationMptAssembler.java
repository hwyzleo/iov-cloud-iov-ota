package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleActivityRelationMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleTaskRelationMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;

import java.util.List;

/**
 * 车辆升级关联查询结果到管理后台 VO 的装配（IOV-OTA-DSN-CR-023 §4）
 *
 * <p>数据库状态码在装配层统一转换为枚举名，SQL 不承担展示语义。
 *
 * @author hwyz_leo
 * @since 2026-11-08
 */
public final class VehicleRelationMptAssembler {

    private VehicleRelationMptAssembler() {
    }

    public static VehicleActivityRelationMpt toActivityVo(VehicleActivityRelationResult result) {
        if (result == null) {
            return null;
        }
        return VehicleActivityRelationMpt.builder()
                .activityId(result.getActivityId())
                .activityName(result.getActivityName())
                .activityState(activityStateName(result.getActivityState()))
                .vehicleTaskCount(result.getVehicleTaskCount())
                .latestTaskTime(result.getLatestTaskTime())
                .build();
    }

    public static List<VehicleActivityRelationMpt> toActivityVoList(List<VehicleActivityRelationResult> results) {
        return results == null ? List.of() : results.stream()
                .map(VehicleRelationMptAssembler::toActivityVo)
                .toList();
    }

    public static VehicleTaskRelationMpt toTaskVo(VehicleTaskRelationResult result) {
        if (result == null) {
            return null;
        }
        return VehicleTaskRelationMpt.builder()
                .vehicleTaskId(result.getVehicleTaskId())
                .vehicleTaskStatus(result.getVehicleTaskStatus())
                .resultCode(result.getResultCode())
                .taskId(result.getTaskId())
                .taskName(result.getTaskName())
                .taskPhase(taskPhaseName(result.getTaskPhase()))
                .taskState(taskStateName(result.getTaskState()))
                .activityId(result.getActivityId())
                .activityName(result.getActivityName())
                .activityState(activityStateName(result.getActivityState()))
                .releaseAt(result.getReleaseAt())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .createTime(result.getCreateTime())
                .build();
    }

    public static List<VehicleTaskRelationMpt> toTaskVoList(List<VehicleTaskRelationResult> results) {
        return results == null ? List.of() : results.stream()
                .map(VehicleRelationMptAssembler::toTaskVo)
                .toList();
    }

    private static String activityStateName(Integer code) {
        ActivityState state = ActivityState.valOf(code);
        return state == null ? null : state.name();
    }

    private static String taskPhaseName(Integer code) {
        TaskPhase phase = TaskPhase.valOf(code);
        return phase == null ? null : phase.name();
    }

    private static String taskStateName(Integer code) {
        TaskState state = TaskState.valOf(code);
        return state == null ? null : state.name();
    }
}
