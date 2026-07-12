package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityDependencyGroupPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityDependencyGroupMapper extends BaseMapper<ActivityDependencyGroupPo> {

    @Select("SELECT * FROM tb_activity_dependency_group WHERE activity_id = #{activityId} AND row_valid = 1 ORDER BY group_code ASC")
    List<ActivityDependencyGroupPo> selectByActivityId(@Param("activityId") Long activityId);

}
