package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存版 Task 仓库（集成测试用）。
 *
 * @author hwyz_leo
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<Long, Task> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Task> getById(TaskId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<Task> findByActivityId(ActivityId activityId) {
        return store.values().stream()
                .filter(t -> t.getActivityId().equals(activityId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findReleasedTasks() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Task> findScheduledTasks() {
        return List.of();
    }

    @Override
    public void save(Task task) {
        store.put(task.getId().getValue(), task);
    }

    @Override
    public void lockActivity(Long activityId) {
        // 内存版无行锁语义，no-op
    }

    @Override
    public Long findMaxSequence(Long activityId, TaskPhase phase) {
        return store.values().stream()
                .filter(t -> t.getActivityId().getValue().equals(activityId) && t.getPhase() == phase)
                .map(Task::getSequenceNo)
                .filter(java.util.Objects::nonNull)
                .map(Integer::longValue)
                .max(Long::compareTo)
                .orElse(null);
    }

    @Override
    public List<Task> findByActivityPhaseSequence(Long activityId, TaskPhase phase, Long sequenceNo) {
        return store.values().stream()
                .filter(t -> t.getActivityId().getValue().equals(activityId)
                        && t.getPhase() == phase
                        && t.getSequenceNo() != null
                        && t.getSequenceNo().longValue() == sequenceNo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByActivityPhaseSequence(Long activityId, TaskPhase phase, Long sequenceNo) {
        return !findByActivityPhaseSequence(activityId, phase, sequenceNo).isEmpty();
    }

    @Override
    public boolean isReferencedAsPrevious(Long taskId) {
        return store.values().stream()
                .anyMatch(t -> taskId.equals(t.getPreviousTaskId()));
    }

    @Override
    public boolean scheduleWithOptimisticLock(Task task, Integer expectedRowVersion) {
        store.put(task.getId().getValue(), task);
        return true;
    }

    @Override
    public Integer getRowVersion(TaskId id) {
        return 1;
    }

    @Override
    public void delete(TaskId id) {
        store.remove(id.getValue());
    }

    @Override
    public void deleteAll(List<TaskId> ids) {
        ids.forEach(id -> store.remove(id.getValue()));
    }
}
