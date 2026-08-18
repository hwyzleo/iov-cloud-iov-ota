package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 升级任务表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-10
 */
@Mapper
public interface TaskMapper extends BaseDao<TaskPo, Long> {

    /**
     * 排程更新（乐观锁：必须匹配当前 state 与 rowVersion，防止并发覆盖）
     *
     * @return 影响行数（0 表示乐观锁冲突/状态不符）
     */
    int updateScheduleWithVersion(@org.apache.ibatis.annotations.Param("id") Long id,
                                  @org.apache.ibatis.annotations.Param("expectedState") Integer expectedState,
                                  @org.apache.ibatis.annotations.Param("expectedRowVersion") Integer expectedRowVersion,
                                  @org.apache.ibatis.annotations.Param("releaseTime") java.util.Date releaseTime,
                                  @org.apache.ibatis.annotations.Param("newState") Integer newState);

}
