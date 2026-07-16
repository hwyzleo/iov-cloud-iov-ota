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

}
