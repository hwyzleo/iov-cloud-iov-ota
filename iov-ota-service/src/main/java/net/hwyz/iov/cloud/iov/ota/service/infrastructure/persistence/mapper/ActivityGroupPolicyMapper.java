package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityGroupPolicyPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 活动分组策略表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Mapper
public interface ActivityGroupPolicyMapper extends BaseDao<ActivityGroupPolicyPo, Long> {

    /**
     * 根据升级活动ID查询组策略列表
     *
     * @param activityId 升级活动ID
     * @return 组策略列表
     */
    @Select("SELECT * FROM tb_activity_group_policy WHERE activity_id = #{activityId} AND row_valid = 1 ORDER BY group_no ASC")
    List<ActivityGroupPolicyPo> selectByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据升级活动ID和分组号查询组策略
     *
     * @param activityId 升级活动ID
     * @param groupNo    分组号
     * @return 组策略
     */
    @Select("SELECT * FROM tb_activity_group_policy WHERE activity_id = #{activityId} AND group_no = #{groupNo} AND row_valid = 1")
    ActivityGroupPolicyPo selectByActivityIdAndGroupNo(@Param("activityId") Long activityId, @Param("groupNo") Integer groupNo);

    /**
     * 根据升级活动ID删除所有组策略（逻辑删除）
     *
     * @param activityId 升级活动ID
     * @return 影响行数
     */
    @Update("UPDATE tb_activity_group_policy SET row_valid = 0, modify_time = NOW() WHERE activity_id = #{activityId} AND row_valid = 1")
    int logicalDeleteByActivityId(@Param("activityId") Long activityId);
}