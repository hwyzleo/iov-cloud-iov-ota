package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.CloudEventOutboxPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * OTA→VMD 云服务事件 Outbox DAO（CR-019 §7）
 *
 * @author hwyz_leo
 */
@Mapper
public interface CloudEventOutboxMapper {

    @Insert("INSERT INTO tb_cloud_event_outbox (event_type, business_key, payload_json, topic, vin, publish_state, retry_count, create_time, modify_time) "
            + "VALUES (#{eventType}, #{businessKey}, #{payloadJson}, #{topic}, #{vin}, 'PENDING', 0, now(), now())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CloudEventOutboxPo po);

    @Select("SELECT * FROM tb_cloud_event_outbox WHERE business_key = #{businessKey}")
    CloudEventOutboxPo selectByBusinessKey(@Param("businessKey") String businessKey);

    @Select("SELECT * FROM tb_cloud_event_outbox WHERE publish_state = 'PENDING' "
            + "AND (next_retry_at IS NULL OR next_retry_at <= now()) ORDER BY id LIMIT #{limit}")
    List<CloudEventOutboxPo> selectPendingReady(@Param("limit") int limit);

    @Update("UPDATE tb_cloud_event_outbox SET publish_state = 'PUBLISHING', modify_time = now() "
            + "WHERE id = #{id} AND publish_state = 'PENDING'")
    int claim(@Param("id") Long id);

    @Update("UPDATE tb_cloud_event_outbox SET publish_state = 'PUBLISHED', published_at = now(), modify_time = now() "
            + "WHERE id = #{id}")
    int markPublished(@Param("id") Long id);

    @Update("UPDATE tb_cloud_event_outbox SET publish_state = 'FAILED', retry_count = retry_count + 1, "
            + "last_error = #{reason}, next_retry_at = #{nextRetryAt}, modify_time = now() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("reason") String reason, @Param("nextRetryAt") Date nextRetryAt);

    @Update("UPDATE tb_cloud_event_outbox SET publish_state = 'DEAD', last_error = #{reason}, modify_time = now() "
            + "WHERE id = #{id}")
    int markDead(@Param("id") Long id, @Param("reason") String reason);
}
