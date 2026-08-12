package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * 安装执行控制指令 PO（CR-012 §3、§5.6）
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_vehicle_execution_control")
public class ExecutionControlPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("control_id")
    private String controlId;

    @TableField("execution_id")
    private Long executionId;

    @TableField("control_revision")
    private Integer controlRevision;

    @TableField("action")
    private String action;

    @TableField("scope")
    private String scope;

    @TableField("apply_mode")
    private String applyMode;

    @TableField("reason")
    private String reason;
}
