package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 多任务放行门禁结果（CR-015 §3.2）
 * <p>查询某任务对其下一任务的放行结论；gateState: PASS/FAIL/PENDING。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskReleaseGateResult {

    private Long activityId;

    /** 前序任务ID（被查询的任务） */
    private Long previousTaskId;

    /** 下一任务ID（门禁约束对象） */
    private Long nextTaskId;

    /** 门禁类型：SAME_PHASE/CROSS_PHASE */
    private String gateType;

    /** 门禁状态：PASS/FAIL/PENDING */
    private String gateState;

    /** 门禁阈值快照（JSON） */
    private String gateThresholdSnapshot;

    /** 前序正式报告引用（reportVersion） */
    private String reportRef;

    /** 是否人工放行 */
    private Boolean override;

    /** 审批引用 */
    private String approvalRef;

    /** 决策人 */
    private String decidedBy;

    /** 决策时间 */
    private Instant decidedAt;

    /** 备注/override原因 */
    private String description;
}
