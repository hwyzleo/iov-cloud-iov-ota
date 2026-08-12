package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.util.List;
import java.util.Optional;

/**
 * 车辆任务领域仓库接口（CR-012 §2.2、§8）
 *
 * <p>VehicleTask 持久化主表为重构后的 tb_task_vehicle。
 *
 * @author hwyz_leo
 */
public interface VehicleTaskRepository {

    /**
     * 按车辆任务ID查询。
     */
    Optional<VehicleTask> getById(VehicleTaskId id);

    /**
     * 按任务ID和VIN查询（UK(task_id, vin)）。
     */
    Optional<VehicleTask> getByTaskIdAndVin(TaskId taskId, String vin);

    /**
     * 按VIN查询所有可见/活动车辆任务。
     */
    List<VehicleTask> findVisibleByVin(String vin);

    /**
     * 按任务ID查询所有车辆任务。
     */
    List<VehicleTask> findByTaskId(TaskId taskId);

    /**
     * 保存车辆任务。
     */
    void save(VehicleTask vehicleTask);
}
