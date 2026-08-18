package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆任务授权唯一事实表 DAO（CR-016 §3.2）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskConsentMapper extends BaseMapper<VehicleTaskConsentPo> {

    @Select("SELECT * FROM tb_vehicle_task_consent WHERE consent_receipt_id = #{consentReceiptId} AND row_valid = 1")
    VehicleTaskConsentPo selectByReceiptId(@Param("consentReceiptId") String consentReceiptId);

    @Select("SELECT * FROM tb_vehicle_task_consent WHERE message_id = #{messageId} AND row_valid = 1 LIMIT 1")
    VehicleTaskConsentPo selectByMessageId(@Param("messageId") String messageId);

    @Select("SELECT * FROM tb_vehicle_task_consent WHERE idempotency_key = #{idempotencyKey} AND row_valid = 1 LIMIT 1")
    VehicleTaskConsentPo selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
