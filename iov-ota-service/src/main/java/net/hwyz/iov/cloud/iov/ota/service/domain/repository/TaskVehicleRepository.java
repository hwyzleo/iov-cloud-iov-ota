package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.VehicleTaskSnapshotSeal;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;

import java.util.List;
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
     * <p>CR-012 §2.2：发布即生成 VehicleTask 快照，除 activityId/taskId/vin 外必须写入
     * vehicle_task_status、release_at、执行窗口快照（vt_start_time/vt_end_time）、
     * task_revision 与授权初始态，否则读取侧（VehicleTaskMapper.selectByVin/selectByTaskId
     * 均以 vehicle_task_status IS NOT NULL 为过滤条件）与车端任务检测永远取不到该车辆任务。</p>
     *
     * @param task     已发布的升级任务（提供 activityId、发布时间与执行窗口快照）
     * @param vehicles 车辆VIN集合
     * @param seal     发布期冻结的包清单摘要密封值（CR-024 §4.28.3）
     * @return 实际新增数量
     */
    int batchCreate(Task task, Set<Vin> vehicles, VehicleTaskSnapshotSeal seal);

    /**
     * 包清单摘要为空的任务ID集合（CR-024 §4.28.3 存量回填）。
     *
     * @return 任务ID列表
     */
    List<Long> taskIdsWithoutSnapshotDigest();

    /**
     * 回填任务下车辆任务的包清单摘要与不可变快照（幂等：仅填充仍为空的记录）。
     *
     * @param taskId 任务ID
     * @param seal   发布期密封值
     * @return 受影响车辆任务数
     */
    int backfillSnapshot(Long taskId, VehicleTaskSnapshotSeal seal);

}
