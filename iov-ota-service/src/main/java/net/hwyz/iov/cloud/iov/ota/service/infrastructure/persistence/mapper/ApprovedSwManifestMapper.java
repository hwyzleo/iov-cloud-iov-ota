package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovedSwManifestMapper extends BaseMapper<ApprovedSwManifestPo> {

    @Select("SELECT * FROM tb_approved_sw_manifest WHERE activity_id = #{activityId} AND row_valid = 1 ORDER BY approve_time DESC")
    List<ApprovedSwManifestPo> selectByActivityId(@Param("activityId") Long activityId);

}
