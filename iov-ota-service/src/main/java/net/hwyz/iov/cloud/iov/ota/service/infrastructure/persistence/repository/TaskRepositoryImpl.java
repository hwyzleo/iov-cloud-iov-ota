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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskInstallConditionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskBatchMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskMetricMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskReportMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UserConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UpgradeLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStateLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskApprovalMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleDetailMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleProcessMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleRetryLogMapper;
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
    private final TaskInstallConditionMapper taskInstallConditionMapper;
    private final TaskBatchMapper taskBatchMapper;
    private final TaskMetricMapper taskMetricMapper;
    private final TaskReportMapper taskReportMapper;
    private final UserConsentMapper userConsentMapper;
    private final UpgradeLogMapper upgradeLogMapper;
    private final TaskStateLogMapper taskStateLogMapper;
    private final TaskApprovalMapper taskApprovalMapper;
    private final TaskVehicleDetailMapper taskVehicleDetailMapper;
    private final TaskVehicleProcessMapper taskVehicleProcessMapper;
    private final TaskVehicleRetryLogMapper taskVehicleRetryLogMapper;
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
    public List<Task> findScheduledTasks() {
        List<TaskPo> taskPoList = taskMapper.selectPoByMap(java.util.Map.of("state", TaskState.SCHEDULED.value));
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
        deleteAll(List.of(id));
    }

    @Override
    public void deleteAll(List<TaskId> ids) {
        List<Long> idValues = ids.stream()
            .map(TaskId::getValue)
            .collect(Collectors.toList());

        for (Long taskId : idValues) {
            List<Long> vehicleIds = taskVehicleMapper.selectIdsByTaskId(taskId);
            if (vehicleIds != null && !vehicleIds.isEmpty()) {
                taskVehicleDetailMapper.deleteByVehicleIds(vehicleIds);
            }

            taskVehicleProcessMapper.deleteByTaskId(taskId);
            taskVehicleRetryLogMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo::getTaskId, taskId));
            taskVehicleMapper.deleteByTaskId(taskId);

            taskRestrictionMapper.deleteByTaskId(taskId);
            taskStrategyMapper.deleteByTaskId(taskId);
            taskInstallConditionMapper.deleteByTaskId(taskId);
            taskBatchMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskBatchPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskBatchPo::getTaskId, taskId));
            taskMetricMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskMetricPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskMetricPo::getTaskId, taskId));
            taskReportMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReportPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReportPo::getTaskId, taskId));
            userConsentMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo::getTaskId, taskId));
            upgradeLogMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradeLogPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradeLogPo::getTaskId, taskId));
            taskStateLogMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo::getTaskId, taskId));
            taskApprovalMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskApprovalPo>()
                    .eq(net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskApprovalPo::getTaskId, taskId));
        }

        taskMapper.batchPhysicalDeletePo(idValues.toArray(new Long[0]));
        idValues.forEach(cacheService::removeTask);
    }
}