package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskVehicleState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.assembler.TaskPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskRestrictionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStrategyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;
    private final TaskRestrictionMapper taskRestrictionMapper;
    private final TaskStrategyMapper taskStrategyMapper;
    private final TaskVehicleMapper taskVehicleMapper;
    private final TaskPoAssembler taskPoAssembler;
    private final CacheService cacheService;

    @Override
    public Optional<Task> getById(TaskId id) {
        return Optional.ofNullable(cacheService.getTask(id.getValue()).orElseGet(() -> {
            TaskPo taskPo = taskMapper.selectPoById(id.getValue());
            if (taskPo == null) {
                return null;
            }
            List<TaskRestrictionPo> restrictionPoList = taskRestrictionMapper.selectPoByTaskId(id.getValue());
            List<TaskStrategyPo> strategyPoList = taskStrategyMapper.selectPoByTaskId(id.getValue());
            Task task = taskPoAssembler.fromPo(taskPo, restrictionPoList, strategyPoList);
            cacheService.setTask(task);
            return task;
        }));
    }

    @Override
    public List<Task> findByActivityId(ActivityId activityId) {
        List<TaskPo> taskPoList = taskMapper.selectPoByMap(java.util.Map.of("activityId", activityId.getValue()));
        return taskPoList.stream()
            .map(po -> {
                List<TaskRestrictionPo> restrictionPoList = taskRestrictionMapper.selectPoByTaskId(po.getId());
                List<TaskStrategyPo> strategyPoList = taskStrategyMapper.selectPoByTaskId(po.getId());
                return taskPoAssembler.fromPo(po, restrictionPoList, strategyPoList);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Task> findReleasedTasks() {
        List<TaskPo> taskPoList = taskMapper.selectPoByMap(java.util.Map.of("state", TaskState.RELEASED.value));
        return taskPoList.stream()
            .map(po -> {
                List<TaskRestrictionPo> restrictionPoList = taskRestrictionMapper.selectPoByTaskId(po.getId());
                List<TaskStrategyPo> strategyPoList = taskStrategyMapper.selectPoByTaskId(po.getId());
                return taskPoAssembler.fromPo(po, restrictionPoList, strategyPoList);
            })
            .collect(Collectors.toList());
    }

    @Override
    public void save(Task task) {
        TaskPo taskPo = taskPoAssembler.toTaskPo(task);
        List<TaskRestrictionPo> restrictionPoList = taskPoAssembler.toRestrictionPoList(task);
        List<TaskStrategyPo> strategyPoList = taskPoAssembler.toStrategyPoList(task);

        if (taskMapper.selectPoById(task.getId().getValue()) == null) {
            taskMapper.insertPo(taskPo);
        } else {
            taskMapper.updatePo(taskPo);
        }

        if (task.getVehicles() != null) {
            task.getVehicles().forEach(vin -> {
                TaskVehiclePo taskVehiclePo = taskVehicleMapper.selectByTaskIdAndVin(taskPo.getId(), vin.getValue());
                if (taskVehiclePo == null) {
                    taskVehicleMapper.insertPo(TaskVehiclePo.builder()
                        .activityId(taskPo.getActivityId())
                        .taskId(taskPo.getId())
                        .vin(vin.getValue())
                        .state(TaskVehicleState.WAITING_DOWNLOAD.value)
                        .build());
                }
            });
        }

        restrictionPoList.forEach(po -> {
            if (po.getId() != null) {
                taskRestrictionMapper.updatePo(po);
            } else {
                taskRestrictionMapper.insertPo(po);
            }
        });

        strategyPoList.forEach(po -> {
            if (po.getId() != null) {
                taskStrategyMapper.updatePo(po);
            } else {
                taskStrategyMapper.insertPo(po);
            }
        });

        cacheService.setTask(task);
    }

    @Override
    public void delete(TaskId id) {
        taskMapper.physicalDeletePo(id.getValue());
        cacheService.removeTask(id.getValue());
    }

    @Override
    public void deleteAll(List<TaskId> ids) {
        List<Long> idValues = ids.stream()
            .map(TaskId::getValue)
            .collect(Collectors.toList());
        taskMapper.batchPhysicalDeletePo(idValues.toArray(new Long[0]));
        idValues.forEach(cacheService::removeTask);
    }
}