package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivitySoftwareBuildVersionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 升级活动软件内部版本关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-09
 */
@Mapper
public interface ActivitySoftwareBuildVersionMapper extends BaseDao<ActivitySoftwareBuildVersionPo, Long> {

    /**
     * 批量物理删除指定活动ID下软件内部版本关系
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoByActivityIdAndSoftwareBuildVersionIds(Long activityId, Long[] softwareBuildVersionIds);

    /**
     * 根据升级活动ID查询软件内部版本关系
     * @param activityId 升级活动ID
     * @return 软件内部版本关系
     */
    List<ActivitySoftwareBuildVersionPo> selectPoByActivityId(Long activityId);

    /**
     * 根据升级活动ID统计数量
     *
     * @param activityId 升级活动ID
     * @return 数量
     */
    int countByActivityId(Long activityId);

}
