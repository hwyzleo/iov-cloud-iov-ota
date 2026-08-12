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
 * 安装执行控制回执 PO（CR-012 §3、§5.6）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_control_ack")
public class ExecutionControlAckPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("control_ack_id")
    private String controlAckId;

    @TableField("control_id")
    private String controlId;

    @TableField("execution_id")
    private Long executionId;

    @TableField("ack_sequence_no")
    private Integer ackSequenceNo;

    @TableField("ack_status")
    private String ackStatus;

    @TableField("ack_payload")
    private String ackPayload;

    @TableField("ack_time")
    private Date ackTime;
}
