package net.hwyz.iov.cloud.iov.ota.service.integration.support;

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
    public void delete(TaskId id) {
        store.remove(id.getValue());
    }

    @Override
    public void deleteAll(List<TaskId> ids) {
        ids.forEach(id -> store.remove(id.getValue()));
    }
}
