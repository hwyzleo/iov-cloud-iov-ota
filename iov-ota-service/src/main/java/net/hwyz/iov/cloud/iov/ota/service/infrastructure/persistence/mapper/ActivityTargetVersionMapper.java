package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityTargetVersionMapper extends BaseDao<ActivityTargetVersionPo, Long> {

    @Select("SELECT * FROM tb_activity_target_version WHERE activity_id = #{activityId} AND row_valid = 1")
    List<ActivityTargetVersionPo> selectByActivityId(@Param("activityId") Long activityId);

}
