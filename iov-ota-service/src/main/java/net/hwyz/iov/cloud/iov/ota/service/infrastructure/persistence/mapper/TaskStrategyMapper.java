package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 升级任务策略表 DAO
 *
 * @author hwyz_leo
 * @since 2026-01-21
 */
@Mapper
public interface TaskStrategyMapper extends BaseDao<TaskStrategyPo, Long> {

    List<TaskStrategyPo> selectPoByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

}
