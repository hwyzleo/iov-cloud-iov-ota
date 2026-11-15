package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 升级任务车辆表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-10
 */
@Mapper
public interface TaskVehicleMapper extends BaseDao<TaskVehiclePo, Long> {

    /**
     * 根据任务ID和车架号查询升级任务车辆
     *
     * @param taskId 任务ID
     * @param vin    车架号
     * @return 升级任务车辆
     */
    TaskVehiclePo selectByTaskIdAndVin(Long taskId, String vin);

    List<Long> selectIdsByTaskId(Long taskId);

    /**
     * 按任务ID查询车辆任务 ID 与 VIN（CR-024 §4.28.3 冻结快照，避免 N+1）
     */
    List<TaskVehiclePo> selectIdVinByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

    /**
     * 包清单摘要为空的任务ID集合（CR-024 §4.28.3 存量回填）
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT DISTINCT task_id FROM tb_task_vehicle "
                    + "WHERE row_valid = 1 AND (snapshot_digest IS NULL OR snapshot_digest = '')")
    List<Long> selectTaskIdsWithoutSnapshotDigest();

    /**
     * 回填任务下车辆任务的包清单摘要（仅更新仍为空的记录，幂等）
     */
    @org.apache.ibatis.annotations.Update(
            "UPDATE tb_task_vehicle SET snapshot_digest = #{digest}, modify_time = CURRENT_TIMESTAMP(3) "
                    + "WHERE task_id = #{taskId} AND row_valid = 1 "
                    + "AND (snapshot_digest IS NULL OR snapshot_digest = '')")
    int updateSnapshotDigestByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId,
                                     @org.apache.ibatis.annotations.Param("digest") String digest);

    void deleteByTaskId(Long taskId);

    /**
     * 待重置时插入一条初始化状态的车辆任务（IOV-OTA-DSN-CR-025 §4.29.5）。
     *
     * <p>沿用发布期的行初始化口径（含冻结包清单摘要），主键由 AUTO_INCREMENT 生成。</p>
     */
    int insertResetTarget(TaskVehiclePo po);

    /**
     * 统计任务下指定车辆任务状态的车辆数（CR-015 指标聚合）。
     *
     * <p>CR-025 §4.29.7：口径限定为当前车辆任务（{@code superseded_by IS NULL}），
     * 被取代的历史行不得重复计入分母与终态计数。</p>
     */
    int countByTaskIdAndVehicleTaskStatus(@org.apache.ibatis.annotations.Param("taskId") Long taskId,
                                          @org.apache.ibatis.annotations.Param("status") String status);

    /**
     * 统计任务下车辆任务总数（CR-015 指标聚合，口径为当前车辆任务）。
     */
    int countAllByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

    /**
     * 统计任务下超时执行数（CR-015，关联 tb_task_vehicle_execution；
     * CR-025 口径限定为当前车辆任务）。
     */
    int countTimeoutExecutionByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

}
