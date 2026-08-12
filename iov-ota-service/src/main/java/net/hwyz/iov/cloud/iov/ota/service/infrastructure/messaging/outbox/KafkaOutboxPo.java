package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * Kafka 下行消息 Outbox PO（CR-013 §6）
 *
 * <p>下行结果、命令和回执可靠生产；由独立发布器轮询 PENDING 行并生产到 Kafka。
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

    @TableField("message_type")
    private String messageType;

    @TableField("message_key")
    private String messageKey;

    @TableField("correlation_id")
    private String correlationId;

    @TableField("vin")
    private String vin;

    @TableField("payload_json")
    private String payloadJson;

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
