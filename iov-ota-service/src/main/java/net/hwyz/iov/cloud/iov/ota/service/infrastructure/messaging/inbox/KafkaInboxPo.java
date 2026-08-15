package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * Kafka 上行消息 Inbox PO（CR-014 §8）
 *
 * <p>UK(consumer_name, message_id) 幂等，envelope_sha256 区分「同 message_id 同摘要(幂等)」
 * 与「同 message_id 异摘要(冲突隔离)」。保存 payload_type/message_kind/protocol_version/vin 供观测。
 * 旧 CR-013 的 business_key/message_type/schema_version/payload_digest 已迁移为 legacy_* 列，禁止新写。
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

    /** 消费者/处理通道标识（=payload_type） */
    @TableField("consumer_name")
    private String consumerName;

    /** 传输消息唯一 ID */
    @TableField("message_id")
    private String messageId;

    /** raw Envelope SHA-256（幂等判定） */
    @TableField("envelope_sha256")
    private String envelopeSha256;

    /** 全限定 payload_type */
    @TableField("payload_type")
    private String payloadType;

    /** REQUEST/RESPONSE/EVENT */
    @TableField("message_kind")
    private String messageKind;

    /** protocol version */
    @TableField("protocol_version")
    private String protocolVersion;

    /** 车架号 */
    @TableField("vin")
    private String vin;

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
