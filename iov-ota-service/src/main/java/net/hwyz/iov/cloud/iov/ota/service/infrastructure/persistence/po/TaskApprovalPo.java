package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

/**
 * 任务审批链留痕持久化对象
 * 对应表：tb_task_approval
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_approval")
public class TaskApprovalPo extends BasePo {

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
     * 审批级别：QUALITY/PRODUCT/SECURITY
     */
    @TableField("level")
    private String level;

    /**
     * 审批人
     */
    @TableField("approver")
    private String approver;

    /**
     * 审批结果：APPROVED/REJECTED
     */
    @TableField("result")
    private String result;

    /**
     * 审批意见
     */
    @TableField("comment")
    private String comment;

    /**
     * 审批时间
     */
    @TableField("decided_at")
    private LocalDateTime decidedAt;

    /**
     * 审批引用（跳阶授权时）
     */
    @TableField("approval_ref")
    private String approvalRef;
}