package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleDetailPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 升级任务车辆详情表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-21
 */
@Mapper
public interface TaskVehicleDetailMapper extends BaseDao<TaskVehicleDetailPo, Long> {

}
