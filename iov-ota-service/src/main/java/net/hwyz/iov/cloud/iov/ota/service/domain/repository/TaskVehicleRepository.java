package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;

import java.util.Optional;

/**
 * 升级任务车辆领域仓库接口
 *
 * @author hwyz_leo
 */
public interface TaskVehicleRepository extends BaseRepository<Long, TaskVehicleDo> {

    /**
     * 根据任务ID和车辆VIN查询升级任务车辆
     *
     * @param taskId 任务ID
     * @param vin    车辆VIN
     * @return 升级任务车辆
     */
    Optional<TaskVehicleDo> getByTaskIdAndVin(Long taskId, String vin);

}
