package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Kafka 上行消息 Inbox DAO（CR-013 §6）
 *
 * @author hwyz_leo
 */
@Mapper
public interface KafkaInboxMapper {

    @Insert("INSERT INTO tb_kafka_message_inbox "
            + "(consumer_name, business_key, message_id, message_type, schema_version, payload_digest, "
            + " kafka_topic, kafka_partition, kafka_offset, status, result_message_id, error_reason, create_time, modify_time) "
            + "VALUES "
            + "(#{consumerName}, #{businessKey}, #{messageId}, #{messageType}, #{schemaVersion}, #{payloadDigest}, "
            + " #{kafkaTopic}, #{kafkaPartition}, #{kafkaOffset}, #{status}, #{resultMessageId}, #{errorReason}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KafkaInboxPo po);

    @Select("SELECT * FROM tb_kafka_message_inbox WHERE consumer_name = #{consumerName} AND business_key = #{businessKey} FOR UPDATE")
    KafkaInboxPo selectForUpdate(@Param("consumerName") String consumerName, @Param("businessKey") String businessKey);

    @Select("SELECT * FROM tb_kafka_message_inbox WHERE consumer_name = #{consumerName} AND business_key = #{businessKey}")
    KafkaInboxPo selectByConsumerAndBusinessKey(@Param("consumerName") String consumerName, @Param("businessKey") String businessKey);

    @Update("UPDATE tb_kafka_message_inbox SET status = #{status}, result_message_id = #{resultMessageId}, "
            + "error_reason = #{errorReason}, modify_time = now() "
            + "WHERE consumer_name = #{consumerName} AND business_key = #{businessKey}")
    int updateProcessResult(@Param("consumerName") String consumerName, @Param("businessKey") String businessKey,
                            @Param("status") String status, @Param("resultMessageId") Long resultMessageId,
                            @Param("errorReason") String errorReason);
}
