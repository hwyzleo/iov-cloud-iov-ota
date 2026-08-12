package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * Kafka 上行消息 Inbox PO（CR-013 §6）
 *
 * <p>上行 Kafka 消息幂等与处理结果索引，UK(consumer_name, business_key)。
 * 保存 message_id/message_type/schema_version/digest/topic/partition/offset/status/result_message_id。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_kafka_message_inbox")
public class KafkaInboxPo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("consumer_name")
    private String consumerName;

    @TableField("business_key")
    private String businessKey;

    @TableField("message_id")
    private String messageId;

    @TableField("message_type")
    private String messageType;

    @TableField("schema_version")
    private Integer schemaVersion;

    @TableField("payload_digest")
    private String payloadDigest;

    @TableField("kafka_topic")
    private String kafkaTopic;

    @TableField("kafka_partition")
    private Integer kafkaPartition;

    @TableField("kafka_offset")
    private Long kafkaOffset;

    @TableField("status")
    private String status;

    @TableField("result_message_id")
    private Long resultMessageId;

    @TableField("error_reason")
    private String errorReason;

    @TableField("create_time")
    private Date createTime;

    @TableField("modify_time")
    private Date modifyTime;

    /** Inbox 处理状态 */
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_CONFLICT = "CONFLICT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DLQ = "DLQ";
}
