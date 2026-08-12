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

    @TableField("received_time")
    private Date receivedTime;
}
