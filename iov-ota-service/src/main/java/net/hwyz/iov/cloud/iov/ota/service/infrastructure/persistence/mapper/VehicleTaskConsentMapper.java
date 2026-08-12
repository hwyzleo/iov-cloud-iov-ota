package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆任务授权凭据 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskConsentMapper extends BaseMapper<VehicleTaskConsentPo> {

    @Select("SELECT * FROM tb_vehicle_task_consent WHERE consent_receipt_id = #{consentReceiptId} AND row_valid = 1")
    VehicleTaskConsentPo selectByReceiptId(@Param("consentReceiptId") String consentReceiptId);
}
