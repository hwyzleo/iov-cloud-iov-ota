package net.hwyz.iov.cloud.iov.ota.service.domain.task.service;

import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;

import java.util.Optional;

/**
 * 升级任务领域服务接口
 *
 * @author hwyz_leo
 */
public interface TaskService {

    /**
     * 获取车辆升级任务
     * 已发布的最早的未完成的任务
     *
     * @param vehicle 车辆
     * @return 升级任务
     */
    Optional<TaskDo> getVehicleTask(VehicleDo vehicle);

}
