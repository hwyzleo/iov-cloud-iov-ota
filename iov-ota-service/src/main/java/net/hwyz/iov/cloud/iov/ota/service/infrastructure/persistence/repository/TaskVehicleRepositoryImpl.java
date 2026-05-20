package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.taskvehicle.model.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.taskvehicle.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler.TaskVehiclePoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskVehicleDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.TaskVehicleDetailDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehicleDetailPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehiclePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 升级任务车辆仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskVehicleRepositoryImpl extends AbstractRepository<Long, TaskVehicleDo> implements TaskVehicleRepository {

    private final TaskVehicleDao taskVehicleDao;
    private final TaskVehicleDetailDao taskVehicleDetailDao;

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

}
