package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskRestrictionPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 升级任务限制条件表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-25
 */
@Mapper
public interface TaskRestrictionMapper extends BaseDao<TaskRestrictionPo, Long> {

    /**
     * 根据任务ID查询限制条件
     *
     * @param taskId 任务ID
     * @return 限制条件列表
     */
    List<TaskRestrictionPo> selectPoByTaskId(Long taskId);

}
