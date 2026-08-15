package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Kafka 上行消息 Inbox DAO（CR-014 §8）
 *
 * @author hwyz_leo
 */
@Mapper
public interface KafkaInboxMapper {

    @Insert("INSERT INTO tb_kafka_message_inbox "
            + "(consumer_name, message_id, envelope_sha256, payload_type, message_kind, protocol_version, vin, "
            + " kafka_topic, kafka_partition, kafka_offset, status, result_message_id, error_reason, create_time, modify_time) "
            + "VALUES "
            + "(#{consumerName}, #{messageId}, #{envelopeSha256}, #{payloadType}, #{messageKind}, #{protocolVersion}, #{vin}, "
            + " #{kafkaTopic}, #{kafkaPartition}, #{kafkaOffset}, #{status}, #{resultMessageId}, #{errorReason}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KafkaInboxPo po);

    @Select("SELECT * FROM tb_kafka_message_inbox WHERE consumer_name = #{consumerName} AND message_id = #{messageId} FOR UPDATE")
    KafkaInboxPo selectForUpdate(@Param("consumerName") String consumerName, @Param("messageId") String messageId);

    @Select("SELECT * FROM tb_kafka_message_inbox WHERE consumer_name = #{consumerName} AND message_id = #{messageId}")
    KafkaInboxPo selectByConsumerAndMessageId(@Param("consumerName") String consumerName, @Param("messageId") String messageId);

    @Update("UPDATE tb_kafka_message_inbox SET status = 'CONFLICT', error_reason = #{reason}, modify_time = now() "
            + "WHERE consumer_name = #{consumerName} AND message_id = #{messageId} AND status = 'PROCESSED'")
    int markConflict(@Param("consumerName") String consumerName, @Param("messageId") String messageId,
                     @Param("reason") String reason);
}
