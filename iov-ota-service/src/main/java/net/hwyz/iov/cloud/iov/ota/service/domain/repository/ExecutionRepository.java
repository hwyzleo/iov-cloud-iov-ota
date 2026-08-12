package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

import java.util.Optional;

/**
 * 安装执行领域仓库接口（CR-012 §2.3、§8）
 *
 * <p>Execution 持久化主表为 tb_task_vehicle_execution；活动执行占位表为 tb_task_vehicle_execution_active。
 * UK(vehicle_task_id, attempt_no)；同一 VehicleTask 同时最多一个活动 Execution。
 *
 * @author hwyz_leo
 */
public interface ExecutionRepository {

    /**
     * 按执行ID查询。
     */
    Optional<Execution> getById(ExecutionId id);

    /**
     * 按车辆任务ID和尝试序号查询（UK(vehicle_task_id, attempt_no)）。
     */
    Optional<Execution> getByVehicleTaskIdAndAttemptNo(VehicleTaskId vehicleTaskId, int attemptNo);

    /**
     * 查询车辆任务当前活动执行（占位表）。
     *
     * @return 活动执行；无则 empty
     */
    Optional<Execution> findActiveByVehicleTaskId(VehicleTaskId vehicleTaskId);

    /**
     * 保存执行（同事务维护活动占位表）。
     */
    void save(Execution execution);
}
