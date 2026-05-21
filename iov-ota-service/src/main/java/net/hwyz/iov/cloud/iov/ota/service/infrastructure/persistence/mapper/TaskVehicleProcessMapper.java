package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehicleProcessPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 升级任务车辆升级过程表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-28
 */
@Mapper
public interface TaskVehicleProcessMapper extends BaseDao<TaskVehicleProcessPo, Long> {

}
