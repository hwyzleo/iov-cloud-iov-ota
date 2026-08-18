package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;

import java.util.List;
import java.util.Optional;

/**
 * 多任务放行门禁仓储接口（CR-015）
 *
 * @author hwyz_leo
 */
public interface TaskReleaseGateRepository {

    Optional<TaskReleaseGate> getById(Long id);

    /** 按被约束的下一任务ID查询放行门禁 */
    Optional<TaskReleaseGate> getByNextTaskId(Long nextTaskId);

    /** 查询某任务对其下一任务的放行结论（previous_task_id = 该任务） */
    Optional<TaskReleaseGate> getByPreviousTaskId(Long previousTaskId);

    /** 查询某任务对其下一任务的放行结论（含 gateState 过滤） */
    List<TaskReleaseGate> listByPreviousTaskIdAndGateState(Long previousTaskId, String gateState);

    List<TaskReleaseGate> listByActivityId(Long activityId);

    TaskReleaseGate save(TaskReleaseGate entity);

    void deleteById(Long id);
}
