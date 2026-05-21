package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityFixedConfigWordPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 升级活动固定配置字表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-23
 */
@Mapper
public interface ActivityFixedConfigWordMapper extends BaseDao<ActivityFixedConfigWordPo, Long> {

    /**
     * 根据升级活动ID查询固定配置字关系
     *
     * @param activityId 升级活动ID
     * @return 固定配置字关系
     */
    List<ActivityFixedConfigWordPo> selectPoByActivityId(Long activityId);

    /**
     * 批量物理删除指定活动ID下固定配置字关系
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoByActivityIdAndFixedConfigWordIds(Long activityId, Long[] fixedConfigWordIds);

}
