package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;

import java.util.List;
import java.util.Optional;

/**
 * 升级任务领域仓库接口
 * 返回Task聚合根
 *
 * @author hwyz_leo
 */
public interface TaskRepository {
    
    Optional<Task> getById(TaskId id);
    
    List<Task> findByActivityId(ActivityId activityId);
    
    List<Task> findReleasedTasks();
    
    List<Task> findScheduledTasks();
    
    void save(Task task);

    /**
     * 排程更新（乐观锁：更新条件含当前 state 与 rowVersion，冲突返回 false）
     */
    boolean scheduleWithOptimisticLock(Task task, Integer expectedRowVersion);

    /**
     * 获取任务当前 rowVersion（乐观锁基准）
     */
    Integer getRowVersion(TaskId id);

    void delete(TaskId id);
    
    void deleteAll(List<TaskId> ids);
}
