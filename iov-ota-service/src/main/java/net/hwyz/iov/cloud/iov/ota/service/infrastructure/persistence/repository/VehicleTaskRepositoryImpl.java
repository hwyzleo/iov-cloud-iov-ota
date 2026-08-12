package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehicleTaskConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 车辆任务仓库实现（CR-012 §2.2、§8）
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleTaskRepositoryImpl implements VehicleTaskRepository {

    private final VehicleTaskMapper vehicleTaskMapper;
    private final VehicleTaskConverter converter;

    @Override
    public Optional<VehicleTask> getById(VehicleTaskId id) {
        TaskVehiclePo po = vehicleTaskMapper.selectById(id.getValue());
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<VehicleTask> getByTaskIdAndVin(TaskId taskId, String vin) {
        TaskVehiclePo po = vehicleTaskMapper.selectByTaskIdAndVin(taskId.getValue(), vin);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<VehicleTask> findVisibleByVin(String vin) {
        return vehicleTaskMapper.selectByVin(vin).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleTask> findByTaskId(TaskId taskId) {
        return vehicleTaskMapper.selectByTaskId(taskId.getValue()).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(VehicleTask vehicleTask) {
        TaskVehiclePo po = converter.toPo(vehicleTask);
        if (po.getId() != null && vehicleTaskMapper.selectById(po.getId()) != null) {
            vehicleTaskMapper.updateById(po);
        } else {
            vehicleTaskMapper.insert(po);
        }
    }
}
