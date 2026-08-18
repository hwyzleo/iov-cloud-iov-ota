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
 * 多任务放行门禁持久化对象（CR-015）
 * 对应表：tb_task_release_gate
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_release_gate")
public class TaskReleaseGatePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 升级活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 前序任务ID
     */
    @TableField("previous_task_id")
    private Long previousTaskId;

    /**
     * 下一任务ID
     */
    @TableField("next_task_id")
    private Long nextTaskId;

    /**
     * 门禁类型：SAME_PHASE/CROSS_PHASE
     */
    @TableField("gate_type")
    private String gateType;

    /**
     * 门禁状态：PASS/FAIL/PENDING
     */
    @TableField("gate_state")
    private String gateState;

    /**
     * 门禁阈值快照（JSON）
     */
    @TableField("gate_threshold_snapshot")
    private String gateThresholdSnapshot;

    /**
     * 前序正式报告引用
     */
    @TableField("report_ref")
    private String reportRef;

    /**
     * 是否人工放行
     */
    @TableField("override")
    private Boolean override;

    /**
     * 审批引用（override时）
     */
    @TableField("approval_ref")
    private String approvalRef;

    /**
     * 决策人
     */
    @TableField("decided_by")
    private String decidedBy;

    /**
     * 决策时间
     */
    @TableField("decided_at")
    private LocalDateTime decidedAt;
}
