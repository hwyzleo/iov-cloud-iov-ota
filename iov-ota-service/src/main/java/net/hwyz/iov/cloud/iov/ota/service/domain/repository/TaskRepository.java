package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
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
     * 锁定 Activity 聚合根（IOV-OTA-DSN-CR-017）
     * <p>在 Task 创建事务内先执行 {@code SELECT id FROM tb_activity WHERE id = ? FOR UPDATE}，
     * 以 Activity 粒度串行化同一 Activity 的 Task 创建，保证并发排号唯一、链无分叉。</p>
     */
    void lockActivity(Long activityId);

    /**
     * 查询同一 (activityId, phase) 作用域内最大波次序（IOV-OTA-DSN-CR-017）
     * <p>包含已取消／已取代及软删除但仍保留审计的历史序号，防止序号复用；无任何记录时返回 null。</p>
     */
    Long findMaxSequence(Long activityId, TaskPhase phase);

    /**
     * 按 (activityId, phase, sequenceNo) 查询候选 Task（IOV-OTA-DSN-CR-017）
     * <p>用于推导前序：唯一候选才可绑定；空或多余候选须 fail-closed。</p>
     */
    List<Task> findByActivityPhaseSequence(Long activityId, TaskPhase phase, Long sequenceNo);

    /**
     * 判断 (activityId, phase, sequenceNo) 是否已存在（IOV-OTA-DSN-CR-017）
     */
    boolean existsByActivityPhaseSequence(Long activityId, TaskPhase phase, Long sequenceNo);

    /**
     * 判断 Task 是否被后续任务作为前序引用（IOV-OTA-DSN-CR-017）
     * <p>被引用的 Task 只能取消，不得物理删除并重排。</p>
     */
    boolean isReferencedAsPrevious(Long taskId);

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
