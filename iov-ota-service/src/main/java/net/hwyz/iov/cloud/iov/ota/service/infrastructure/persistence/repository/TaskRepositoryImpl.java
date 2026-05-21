package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskVehicleState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestrictionVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.TaskPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.TaskRestrictionPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskRestrictionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
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

    private final TaskMapper taskDao;
    private final CacheService cacheService;
    private final TaskVehicleMapper taskVehicleDao;
    private final TaskRestrictionMapper taskRestrictionDao;

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
