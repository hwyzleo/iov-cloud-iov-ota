package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * Kafka 下行消息 Outbox DAO（CR-014 §8）
 *
 * @author hwyz_leo
 */
@Mapper
public interface KafkaOutboxMapper {

    @Insert("INSERT INTO tb_kafka_message_outbox "
            + "(aggregate_type, aggregate_id, message_id, payload_type, message_kind, correlation_id, vin, "
            + " envelope_bytes, envelope_sha256, "
            + " publish_state, retry_count, next_retry_at, last_error, create_time, modify_time) "
            + "VALUES "
            + "(#{aggregateType}, #{aggregateId}, #{messageId}, #{payloadType}, #{messageKind}, #{correlationId}, #{vin}, "
            + " #{envelopeBytes}, #{envelopeSha256}, "
            + " 'PENDING', 0, NULL, NULL, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KafkaOutboxPo po);

    /**
     * 认领待发布且已到重试时间的消息（按 id 升序保证顺序）。
     */
    @Select("SELECT * FROM tb_kafka_message_outbox "
            + "WHERE message_id = #{messageId} ORDER BY id LIMIT 1")
    KafkaOutboxPo selectByMessageId(@Param("messageId") String messageId);

    @Select("SELECT * FROM tb_kafka_message_outbox "
            + "WHERE publish_state = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= now()) "
            + "ORDER BY id LIMIT #{limit}")
    List<KafkaOutboxPo> selectPendingReady(@Param("limit") int limit);

    @Update("UPDATE tb_kafka_message_outbox SET publish_state = 'PUBLISHED', published_at = now(), modify_time = now() WHERE id = #{id}")
    int markPublished(@Param("id") Long id);

    /**
     * 原子认领（PENDING -> PUBLISHING），仅成功认领的行才会被发布，避免重复生产。
     */
    @Update("UPDATE tb_kafka_message_outbox SET publish_state = 'PUBLISHING', modify_time = now() "
            + "WHERE id = #{id} AND publish_state = 'PENDING'")
    int claim(@Param("id") Long id);

    @Update("UPDATE tb_kafka_message_outbox SET publish_state = 'FAILED', retry_count = retry_count + 1, "
            + "last_error = #{reason}, next_retry_at = #{nextRetryAt}, modify_time = now() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("reason") String reason, @Param("nextRetryAt") Date nextRetryAt);

    @Update("UPDATE tb_kafka_message_outbox SET publish_state = 'DEAD', retry_count = retry_count + 1, "
            + "last_error = #{reason}, modify_time = now() WHERE id = #{id}")
    int markDead(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 受控重试：重新入队（PENDING）并写入下次重试时间，retry_count 递增（受上限约束）。
     */
    @Update("UPDATE tb_kafka_message_outbox SET publish_state = 'PENDING', retry_count = retry_count + 1, "
            + "next_retry_at = #{nextRetryAt}, last_error = #{reason}, published_at = NULL, modify_time = now() "
            + "WHERE id = #{id}")
    int requeue(@Param("id") Long id, @Param("nextRetryAt") Date nextRetryAt, @Param("reason") String reason);
}
