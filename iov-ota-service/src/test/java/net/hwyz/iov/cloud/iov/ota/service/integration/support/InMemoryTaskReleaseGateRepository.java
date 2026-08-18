package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CR-015 集成测试内存版 TaskReleaseGate 仓储
 *
 * @author hwyz_leo
 */
public class InMemoryTaskReleaseGateRepository implements TaskReleaseGateRepository {

    private final Map<Long, TaskReleaseGate> store = new HashMap<>();
    private long seq = 1;

    @Override
    public Optional<TaskReleaseGate> getById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<TaskReleaseGate> getByNextTaskId(Long nextTaskId) {
        return store.values().stream()
                .filter(g -> nextTaskId.equals(g.getNextTaskId()))
                .findFirst();
    }

    @Override
    public Optional<TaskReleaseGate> getByPreviousTaskId(Long previousTaskId) {
        return store.values().stream()
                .filter(g -> previousTaskId.equals(g.getPreviousTaskId()))
                .max(Comparator.comparing(TaskReleaseGate::getId));
    }

    @Override
    public List<TaskReleaseGate> listByPreviousTaskIdAndGateState(Long previousTaskId, String gateState) {
        return store.values().stream()
                .filter(g -> previousTaskId.equals(g.getPreviousTaskId()))
                .filter(g -> gateState.equals(g.getGateState() != null ? g.getGateState().getValue() : ReleaseGateState.PENDING.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskReleaseGate> listByActivityId(Long activityId) {
        return store.values().stream()
                .filter(g -> activityId.equals(g.getActivityId()))
                .collect(Collectors.toList());
    }

    @Override
    public TaskReleaseGate save(TaskReleaseGate entity) {
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
