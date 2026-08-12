package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存版 VehicleTask 仓库（集成测试用）。
 *
 * @author hwyz_leo
 */
public class InMemoryVehicleTaskRepository implements VehicleTaskRepository {

    private final Map<Long, VehicleTask> store = new ConcurrentHashMap<>();

    @Override
    public Optional<VehicleTask> getById(VehicleTaskId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public Optional<VehicleTask> getByTaskIdAndVin(TaskId taskId, String vin) {
        return store.values().stream()
                .filter(vt -> vt.getTaskId().equals(taskId.getValue()) && vin.equals(vt.getVin()))
                .findFirst();
    }

    @Override
    public List<VehicleTask> findVisibleByVin(String vin) {
        return store.values().stream()
                .filter(vt -> vin.equals(vt.getVin()))
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleTask> findByTaskId(TaskId taskId) {
        return store.values().stream()
                .filter(vt -> vt.getTaskId().equals(taskId.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void save(VehicleTask vehicleTask) {
        store.put(vehicleTask.getId().getValue(), vehicleTask);
    }

    public List<VehicleTask> all() {
        return new ArrayList<>(store.values());
    }
}
