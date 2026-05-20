package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityCompatiblePnPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 升级活动兼容零件号关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-16
 */
@Mapper
public interface ActivityCompatiblePnMapper extends BaseDao<ActivityCompatiblePnPo, Long> {

    /**
     * 根据升级活动ID查询兼容零件号关系
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号关系
     */
    List<ActivityCompatiblePnPo> selectPoByActivityId(Long activityId);

    /**
     * 批量物理删除指定活动ID下兼容零件号关系
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoByActivityIdAndCompatiblePnIds(Long activityId, Long[] compatiblePnIds);

}
