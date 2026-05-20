package net.hwyz.iov.cloud.iov.ota.service.domain.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.service.TaskService;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 升级任务领域服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final CacheService cacheService;
    private final TaskRepository taskRepository;

    @Override
    public Optional<TaskDo> getVehicleTask(VehicleDo vehicle) {
        AtomicReference<TaskDo> task = new AtomicReference<>();
        cacheService.getVehicleTask(vehicle.getId()).flatMap(taskRepository::getById).ifPresent(task::set);
        return Optional.ofNullable(task.get());
    }

}
