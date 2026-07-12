package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityInstallOrderMapper extends BaseMapper<ActivityInstallOrderPo> {

    @Select("SELECT * FROM tb_activity_install_order WHERE activity_id = #{activityId} AND row_valid = 1 ORDER BY seq_no ASC")
    List<ActivityInstallOrderPo> selectByActivityId(@Param("activityId") Long activityId);

}
