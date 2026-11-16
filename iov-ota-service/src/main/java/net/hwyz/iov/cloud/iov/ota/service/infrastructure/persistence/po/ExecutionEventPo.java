package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.util.Date;

/**
 * 安装执行事件 PO（CR-012 §3、§5.6）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_event")
public class ExecutionEventPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private String eventId;

    @TableField("execution_id")
    private Long executionId;

    @TableField("sequence_no")
    private Long sequenceNo;

    @TableField("event_type")
    private String eventType;

    @TableField("event_digest")
    private String eventDigest;

    @TableField("event_payload")
    private String eventPayload;

    @TableField("disposition")
    private String disposition;

    /** 车端事件发生时间（CR-026） */
    @TableField("source_occurred_at")
    private Date sourceOccurredAt;

    /** 事件携带百分比（CR-026） */
    @TableField("progress_percent")
    private Integer progressPercent;

    @TableField("ecu_id")
    private String ecuId;

    @TableField("software_target_code")
    private String softwareTargetCode;

    @TableField("slot")
    private String slot;

    @TableField("error_code")
    private String errorCode;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("received_time")
    private Date receivedTime;
}
