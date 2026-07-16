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
 * 跨任务阶段门禁持久化对象
 * 对应表：tb_task_phase_gate
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_task_phase_gate")
public class TaskPhaseGatePo extends BasePo {

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
     * 来源阶段：1=VALIDATION, 2=CANARY, 3=RELEASE
     */
    @TableField("from_phase")
    private Integer fromPhase;

    /**
     * 目标阶段：1=VALIDATION, 2=CANARY, 3=RELEASE
     */
    @TableField("to_phase")
    private Integer toPhase;

    /**
     * 前序任务ID
     */
    @TableField("prev_task_id")
    private Long prevTaskId;

    /**
     * 门禁状态：PASS/FAIL/PENDING
     */
    @TableField("gate_state")
    private String gateState;

    /**
     * 是否人工跳阶授权
     */
    @TableField("override")
    private Boolean override;

    /**
     * 审批引用（跳阶授权时）
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

    /**
     * 报告引用
     */
    @TableField("report_ref")
    private String reportRef;

    /**
     * 门禁阈值快照（JSON）
     */
    @TableField("gate_threshold_snapshot")
    private String gateThresholdSnapshot;
}