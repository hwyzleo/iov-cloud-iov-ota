package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegulatoryFilingMapper extends BaseMapper<RegulatoryFilingPo> {

    @Select("SELECT * FROM tb_regulatory_filing WHERE activity_id = #{activityId} AND row_valid = 1")
    List<RegulatoryFilingPo> selectByActivityId(@Param("activityId") Long activityId);

}
