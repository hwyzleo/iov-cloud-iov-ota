package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * VAGW 云内技术投递状态观测 PO（CR-014 §7/§8）
 *
 * <p>UK(original_message_id, stage, occurred_at_ms)；VIN 仅存 hash，不落原文。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_gateway_delivery_observation")
public class GatewayDeliveryObservationPo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("original_message_id")
    private String originalMessageId;

    @TableField("correlation_id")
    private String correlationId;

    @TableField("vin_hash")
    private String vinHash;

    @TableField("stage")
    private String stage;

    @TableField("outcome")
    private String outcome;

    @TableField("reason")
    private String reason;

    @TableField("retryable")
    private Boolean retryable;

    @TableField("retry_after_ms")
    private Long retryAfterMs;

    @TableField("occurred_at_ms")
    private Long occurredAtMs;

    @TableField("received_at")
    private Date receivedAt;

    @TableField("create_time")
    private Date createTime;

    @TableField("modify_time")
    private Date modifyTime;
}
