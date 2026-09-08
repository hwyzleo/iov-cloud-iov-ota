package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * OTA→VMD 云服务事件 Outbox PO（CR-019 §7/§4.23.6）
 *
 * <p>业务事务内写入 payload JSON，独立 Relay 轮询发布到云服务 Topic。
 * UK(business_key) 保证相同观测身份不重复建 Outbox。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_cloud_event_outbox")
public class CloudEventOutboxPo {

    private static final long serialVersionUID = 1L;

    public static final String EVENT_INVENTORY_OBSERVED = "VEHICLE_INVENTORY_OBSERVED";

    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_PUBLISHING = "PUBLISHING";
    public static final String STATE_PUBLISHED = "PUBLISHED";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_DEAD = "DEAD";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("event_type")
    private String eventType;

    @TableField("business_key")
    private String businessKey;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("topic")
    private String topic;

    @TableField("vin")
    private String vin;

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
}
