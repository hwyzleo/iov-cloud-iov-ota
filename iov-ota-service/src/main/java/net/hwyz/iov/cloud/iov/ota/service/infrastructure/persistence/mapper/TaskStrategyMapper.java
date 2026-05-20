package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskStrategyPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 升级任务策略表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-21
 */
@Mapper
public interface TaskStrategyMapper extends BaseDao<TaskStrategyPo, Long> {

}
