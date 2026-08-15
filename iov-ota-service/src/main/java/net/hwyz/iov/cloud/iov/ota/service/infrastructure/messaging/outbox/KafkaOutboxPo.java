package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * Kafka 下行消息 Outbox PO（CR-014 §6/§8）
 *
 * <p>首次创建即冻结完整序列化 Envelope bytes（envelope_bytes + envelope_sha256）；
 * 重试只重发已持久化 bytes，不重建 Envelope、不改 message_id/关联链。
 * 旧 CR-013 的 message_type/message_key/payload_json 已迁移为 legacy_* 列，禁止新写。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_kafka_message_outbox")
public class KafkaOutboxPo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private String aggregateId;

    /** 传输消息唯一 ID（首次创建时生成，重试不变） */
    @TableField("message_id")
    private String messageId;

    /** 全限定 payload_type */
    @TableField("payload_type")
    private String payloadType;

    /** REQUEST/RESPONSE/EVENT */
    @TableField("message_kind")
    private String messageKind;

    /** 关联请求 correlation_id（RESPONSE 指向请求 message_id） */
    @TableField("correlation_id")
    private String correlationId;

    /** 车架号（Kafka Key=Envelope.vin） */
    @TableField("vin")
    private String vin;

    /** 冻结的完整序列化 Envelope bytes（重试复用） */
    @TableField("envelope_bytes")
    private byte[] envelopeBytes;

    /** Envelope bytes SHA-256 */
    @TableField("envelope_sha256")
    private String envelopeSha256;

    @TableField("publish_state")
    private String publishState;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_at")
    private Date nextRetryAt;

    @TableField("last_error")
    private String lastError;

    @TableField("published_at")
    private Date publishedAt;

    @TableField("create_time")
    private Date createTime;

    @TableField("modify_time")
    private Date modifyTime;

    /** Outbox 发布状态 */
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_PUBLISHING = "PUBLISHING";
    public static final String STATE_PUBLISHED = "PUBLISHED";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_DEAD = "DEAD";
}
