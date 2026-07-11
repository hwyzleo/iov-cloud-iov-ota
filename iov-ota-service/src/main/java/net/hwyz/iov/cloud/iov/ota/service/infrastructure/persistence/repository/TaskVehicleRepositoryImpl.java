package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.TaskVehiclePoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleDetailMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleDetailPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 升级任务车辆仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskVehicleRepositoryImpl extends AbstractRepository<Long, TaskVehicleDo> implements TaskVehicleRepository {

    private final TaskVehicleMapper taskVehicleDao;
    private final TaskVehicleDetailMapper taskVehicleDetailDao;

    @Override
    public Optional<TaskVehicleDo> getById(Long id) {
        TaskVehiclePo taskVehiclePo = taskVehicleDao.selectPoById(id);
        return Optional.ofNullable(TaskVehiclePoAssembler.INSTANCE.toDo(taskVehiclePo));
    }

    @Override
    public Optional<TaskVehicleDo> getByTaskIdAndVin(Long taskId, String vin) {
        TaskVehiclePo taskVehiclePo = taskVehicleDao.selectByTaskIdAndVin(taskId, vin);
        return Optional.ofNullable(TaskVehiclePoAssembler.INSTANCE.toDo(taskVehiclePo));
    }

    @Override
    public boolean save(TaskVehicleDo taskVehicleDo) {
        switch (taskVehicleDo.getState()) {
            case CHANGED -> {
                TaskVehiclePo taskVehiclePo = TaskVehiclePoAssembler.INSTANCE.fromDo(taskVehicleDo);
                taskVehicleDao.updatePo(taskVehiclePo);
                TaskVehicleDetailPo taskVehicleDetail = taskVehicleDetailDao.selectPoById(taskVehicleDo.getId());
                if (taskVehicleDetail == null) {
                    taskVehicleDetailDao.insertPo(TaskVehicleDetailPo.builder()
                            .id(taskVehicleDo.getId())
                            .fotaInfo(JSONUtil.toJsonStr(taskVehicleDo.getSoftwareBuildVersionList()))
                            .build());
                } else {
                    taskVehicleDetail.setFotaInfo(JSONUtil.toJsonStr(taskVehicleDo.getSoftwareBuildVersionList()));
                    taskVehicleDetailDao.updatePo(taskVehicleDetail);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int batchCreate(Long taskId, Long activityId, Set<Vin> vehicles) {
        List<TaskVehiclePo> newRecords = new ArrayList<>();
        for (Vin vin : vehicles) {
            TaskVehiclePo existing = taskVehicleDao.selectByTaskIdAndVin(taskId, vin.getValue());
            if (existing == null) {
                newRecords.add(TaskVehiclePo.builder()
                        .activityId(activityId)
                        .taskId(taskId)
                        .vin(vin.getValue())
                        .state(0)
                        .build());
            }
        }
        if (newRecords.isEmpty()) {
            return 0;
        }
        return taskVehicleDao.batchInsertPo(newRecords);
    }

}
