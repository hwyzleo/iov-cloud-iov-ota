package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.TaskVehicleState;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskRestrictionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler.TaskPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler.TaskRestrictionPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskRestrictionDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskVehicleDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 升级任务仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl extends AbstractRepository<Long, TaskDo> implements TaskRepository {

    private final TaskDao taskDao;
    private final CacheService cacheService;
    private final TaskVehicleDao taskVehicleDao;
    private final TaskRestrictionDao taskRestrictionDao;

    @Override
    public Optional<TaskDo> getById(Long id) {
        return Optional.ofNullable(cacheService.getTask(id).orElseGet(() -> {
            TaskPo taskPo = taskDao.selectPoById(id);
            if (taskPo != null) {
                List<TaskRestrictionVo> taskRestrictionList = TaskRestrictionPoAssembler.INSTANCE.toVoList(taskRestrictionDao.selectPoByTaskId(id));
                TaskDo taskDo = TaskPoAssembler.INSTANCE.toDo(taskPo);
                taskDo.load(taskRestrictionList);
                cacheService.setTask(taskDo);
                return taskDo;
            }
            return null;
        }));
    }

    @Override
    public boolean save(TaskDo taskDo) {
        switch (taskDo.getState()) {
            case CHANGED -> {
                TaskPo taskPo = TaskPoAssembler.INSTANCE.fromDo(taskDo);
                taskDao.updatePo(taskPo);
                if (taskDo.getVehicles() != null) {
                    taskDo.getVehicles().forEach(vehicle -> {
                        TaskVehiclePo taskVehiclePo = taskVehicleDao.selectByTaskIdAndVin(taskPo.getId(), vehicle);
                        if (taskVehiclePo == null) {
                            taskVehicleDao.insertPo(TaskVehiclePo.builder()
                                    .activityId(taskPo.getActivityId())
                                    .taskId(taskPo.getId())
                                    .vin(vehicle)
                                    .state(TaskVehicleState.WAITING_DOWNLOAD.value)
                                    .build());
                        }
                    });
                }
                if (taskDo.getTaskRestrictionMap() != null) {
                    taskDo.getTaskRestrictionMap().values().forEach(taskRestriction -> {
                        TaskRestrictionPo taskRestrictionPo = TaskRestrictionPoAssembler.INSTANCE.fromVo(taskRestriction);
                        if (taskRestrictionPo.getId() != null) {
                            taskRestrictionDao.updatePo(taskRestrictionPo);
                        } else {
                            taskRestrictionDao.insertPo(taskRestrictionPo);
                        }
                    });
                }
                cacheService.setTask(taskDo);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

}
