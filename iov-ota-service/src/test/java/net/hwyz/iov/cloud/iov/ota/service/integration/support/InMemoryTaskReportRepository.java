package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CR-015 集成测试内存版 TaskReport 仓储
 *
 * @author hwyz_leo
 */
public class InMemoryTaskReportRepository implements TaskReportRepository {

    private final Map<Long, TaskReport> store = new HashMap<>();
    private long seq = 1;

    @Override
    public Optional<TaskReport> getById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<TaskReport> findLatestByTaskId(Long taskId) {
        return store.values().stream()
                .filter(r -> taskId.equals(r.getTaskId()))
                .max(Comparator.comparing(TaskReport::getReportVersion));
    }

    @Override
    public List<TaskReport> listByTaskId(Long taskId) {
        return store.values().stream()
                .filter(r -> taskId.equals(r.getTaskId()))
                .collect(Collectors.toList());
    }

    @Override
    public TaskReport save(TaskReport entity) {
        if (entity.getId() == null) {
            entity.setId(seq++);
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
