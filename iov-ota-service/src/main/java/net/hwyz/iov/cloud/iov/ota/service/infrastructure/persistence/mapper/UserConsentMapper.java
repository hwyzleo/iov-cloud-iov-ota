package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserConsentMapper extends BaseMapper<UserConsentPo> {

    @Select("SELECT * FROM tb_user_consent WHERE task_id = #{taskId} AND row_valid = 1 ORDER BY consent_time DESC")
    List<UserConsentPo> selectByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM tb_user_consent WHERE vin = #{vin} AND row_valid = 1 ORDER BY consent_time DESC")
    List<UserConsentPo> selectByVin(@Param("vin") String vin);

}
