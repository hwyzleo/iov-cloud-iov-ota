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

    void deleteByTaskId(Long taskId);

    /**
     * 统计任务下指定车辆任务状态的车辆数（CR-015 指标聚合）
     */
    int countByTaskIdAndVehicleTaskStatus(@org.apache.ibatis.annotations.Param("taskId") Long taskId,
                                          @org.apache.ibatis.annotations.Param("status") String status);

    /**
     * 统计任务下车辆任务总数（CR-015 指标聚合）
     */
    int countAllByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

    /**
     * 统计任务下超时执行数（CR-015，关联 tb_task_vehicle_execution）
     */
    int countTimeoutExecutionByTaskId(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

}
