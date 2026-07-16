package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 任务状态迁移审计持久化对象
 * 对应表：tb_task_state_log
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_state_log")
public class TaskStateLogPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 原状态
     */
    @TableField("from_state")
    private Integer fromState;

    /**
     * 新状态
     */
    @TableField("to_state")
    private Integer toState;

    /**
     * 操作：SUBMIT/AUDIT/RELEASE/PAUSE/RESUME/CANCEL/FINISH/SUPERSEDE
     */
    @TableField("action")
    private String action;

    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;

    /**
     * 原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 决策时间
     */
    @TableField("decided_at")
    private LocalDateTime decidedAt;

    /**
     * 备注
     */
    @TableField("description")
    private String description;
}
