package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;

import java.util.Optional;
import java.util.Set;

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

    /**
     * 批量创建升级任务车辆记录（幂等：跳过已存在的 taskId+vin 组合）
     *
     * @param taskId     任务ID
     * @param activityId 活动ID
     * @param vehicles   车辆VIN集合
     * @return 实际新增数量
     */
    int batchCreate(Long taskId, Long activityId, Set<Vin> vehicles);

}
