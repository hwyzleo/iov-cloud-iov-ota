package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 Execution 仓库（集成测试用）。
 *
 * <p>实现活动执行占位逻辑：活动 Execution 时占用 vehicleTaskId，终态时释放。
 *
 * @author hwyz_leo
 */
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<Long, Execution> store = new ConcurrentHashMap<>();
    private final Map<Long, Long> activeByVehicleTask = new ConcurrentHashMap<>();

    @Override
    public Optional<Execution> getById(ExecutionId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public Optional<Execution> getByVehicleTaskIdAndAttemptNo(VehicleTaskId vehicleTaskId, int attemptNo) {
        return store.values().stream()
                .filter(ex -> ex.getVehicleTaskId().equals(vehicleTaskId)
                        && ex.getAttemptNo() == attemptNo)
                .findFirst();
    }

    @Override
    public Optional<Execution> findActiveByVehicleTaskId(VehicleTaskId vehicleTaskId) {
        Long activeId = activeByVehicleTask.get(vehicleTaskId.getValue());
        return activeId == null ? Optional.empty() : Optional.ofNullable(store.get(activeId));
    }

    @Override
    public void save(Execution execution) {
        store.put(execution.getId().getValue(), execution);
        if (execution.isActive()) {
            Long previous = activeByVehicleTask.putIfAbsent(
                    execution.getVehicleTaskId().getValue(), execution.getId().getValue());
            if (previous != null && !previous.equals(execution.getId().getValue())) {
                throw new IllegalStateException("已存在活动执行，占位冲突");
            }
        } else {
            activeByVehicleTask.remove(execution.getVehicleTaskId().getValue());
        }
    }
}
