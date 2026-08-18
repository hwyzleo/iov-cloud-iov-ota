package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.GatewayDeliveryObservationPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * VAGW 技术投递状态观测 DAO（CR-014 §8）
 *
 * @author hwyz_leo
 */
@Mapper
public interface GatewayDeliveryObservationMapper {

    @Insert("INSERT INTO tb_gateway_delivery_observation "
            + "(original_message_id, correlation_id, vin_hash, stage, outcome, reason, retryable, "
            + " retry_after_ms, occurred_at_ms, received_at, create_time, modify_time) "
            + "VALUES "
            + "(#{originalMessageId}, #{correlationId}, #{vinHash}, #{stage}, #{outcome}, #{reason}, #{retryable}, "
            + " #{retryAfterMs}, #{occurredAtMs}, now(), now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GatewayDeliveryObservationPo po);

    @Select("SELECT * FROM tb_gateway_delivery_observation "
            + "WHERE original_message_id = #{originalMessageId} AND stage = #{stage} AND occurred_at_ms = #{occurredAtMs}")
    GatewayDeliveryObservationPo selectUnique(@Param("originalMessageId") String originalMessageId,
                                              @Param("stage") String stage,
                                              @Param("occurredAtMs") Long occurredAtMs);

    @Select("SELECT * FROM tb_gateway_delivery_observation "
            + "WHERE vin_hash = #{vinHash} "
            + "ORDER BY occurred_at_ms DESC, id DESC LIMIT #{limit}")
    List<GatewayDeliveryObservationPo> selectByVinHash(@Param("vinHash") String vinHash,
                                                      @Param("limit") int limit);
}
