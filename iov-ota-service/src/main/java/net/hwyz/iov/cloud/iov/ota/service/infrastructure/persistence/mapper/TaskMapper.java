package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
    int updateScheduleWithVersion(@Param("id") Long id,
                                  @Param("expectedState") Integer expectedState,
                                  @Param("expectedRowVersion") Integer expectedRowVersion,
                                  @Param("releaseTime") java.util.Date releaseTime,
                                  @Param("newState") Integer newState);

    /**
     * 锁定 Activity 行（IOV-OTA-DSN-CR-017）：{@code SELECT id FROM tb_activity WHERE id = ? FOR UPDATE}
     */
    Long selectActivityIdForUpdate(@Param("activityId") Long activityId);

    /**
     * 查询 (activityId, phase) 作用域内最大波次序（IOV-OTA-DSN-CR-017）
     * <p>包含已取消/已取代/软删除的历史序号，防止序号复用；无记录返回 null。</p>
     */
    Long selectMaxSequence(@Param("activityId") Long activityId, @Param("phase") Integer phase);

    /**
     * 按 (activityId, phase, sequenceNo) 查询任务列表（IOV-OTA-DSN-CR-017，前序候选推导）
     */
    List<TaskPo> selectByActivityPhaseSequence(@Param("activityId") Long activityId,
                                               @Param("phase") Integer phase,
                                               @Param("sequenceNo") Long sequenceNo);

    /**
     * 判断 (activityId, phase, sequenceNo) 是否已存在（IOV-OTA-DSN-CR-017）
     */
    int countByActivityPhaseSequence(@Param("activityId") Long activityId,
                                     @Param("phase") Integer phase,
                                     @Param("sequenceNo") Long sequenceNo);

    /**
     * 统计被后续任务引用为前序的数量（IOV-OTA-DSN-CR-017，删除守卫）
     */
    int countByPreviousTaskId(@Param("previousTaskId") Long previousTaskId);

}
