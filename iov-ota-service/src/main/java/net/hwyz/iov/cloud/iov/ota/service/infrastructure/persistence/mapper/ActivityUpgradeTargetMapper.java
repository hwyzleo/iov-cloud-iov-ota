package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 活动升级对象表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Mapper
public interface ActivityUpgradeTargetMapper extends BaseDao<ActivityUpgradeTargetPo, Long> {

    /**
     * 批量物理删除指定活动ID下升级对象
     *
     * @param activityId 升级活动ID
     * @param ids        升级对象ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoByActivityIdAndIds(Long activityId, Long[] ids);

    /**
     * 根据升级活动ID查询升级对象
     * @param activityId 升级活动ID
     * @return 升级对象列表
     */
    List<ActivityUpgradeTargetPo> selectPoByActivityId(Long activityId);

    /**
     * 根据升级活动ID统计数量
     *
     * @param activityId 升级活动ID
     * @return 数量
     */
    int countByActivityId(Long activityId);

    /**
     * 根据升级活动ID和分组号查询升级对象
     *
     * @param activityId 升级活动ID
     * @param groupNo    分组号
     * @return 升级对象列表
     */
    List<ActivityUpgradeTargetPo> selectPoByActivityIdAndGroupNo(Long activityId, Integer groupNo);

    /**
     * 根据升级活动ID和来源类型查询升级对象
     *
     * @param activityId 升级活动ID
     * @param sourceType 来源类型
     * @return 升级对象列表
     */
    List<ActivityUpgradeTargetPo> selectPoByActivityIdAndSourceType(Long activityId, Integer sourceType);
}