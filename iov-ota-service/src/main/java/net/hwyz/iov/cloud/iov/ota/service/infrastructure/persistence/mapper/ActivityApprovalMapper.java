package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 升级活动多级审批 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivityApprovalMapper extends BaseMapper<ActivityApprovalPo> {

    @Select("SELECT * FROM tb_activity_approval WHERE activity_id = #{activityId} AND row_valid = 1 ORDER BY approve_time ASC")
    List<ActivityApprovalPo> selectByActivityId(@Param("activityId") Long activityId);

}
