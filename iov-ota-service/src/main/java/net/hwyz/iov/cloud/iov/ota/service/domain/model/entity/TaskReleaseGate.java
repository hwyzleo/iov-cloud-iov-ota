package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;

import java.time.Instant;

/**
 * 多任务放行门禁领域实体（CR-015）
 * 对应表：tb_task_release_gate
 * <p>门禁挂在下一任务（nextTaskId）上；同一 Activity 允许多个相同 phase 的 Task，
 * 每个放量波次创建独立 Task，发布时基于前序正式报告计算/读取放行门禁。</p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
public class TaskReleaseGate {

    private Long id;
    private Long activityId;
    /** 前序任务ID（波次/上一阶段） */
    private Long previousTaskId;
    /** 下一任务ID（被门禁约束的任务） */
    private Long nextTaskId;
    /** 门禁类型：SAME_PHASE(同阶段波次)/CROSS_PHASE(跨阶段推进) */
    private ReleaseGateType gateType;
    /** 门禁状态：PASS/FAIL/PENDING */
    private ReleaseGateState gateState;
    /** 门禁阈值快照（JSON） */
    private String gateThresholdSnapshot;
    /** 前序正式报告引用（reportVersion） */
    private String reportRef;
    /** 是否人工放行 */
    private Boolean override;
    /** 审批引用（override时） */
    private String approvalRef;
    /** 决策人 */
    private String decidedBy;
    /** 决策时间 */
    private Instant decidedAt;
    /** 备注/override原因 */
    private String description;

    /** 门禁类型枚举 */
    public enum ReleaseGateType {
        SAME_PHASE, CROSS_PHASE
    }

    public boolean isPassed() {
        return gateState == ReleaseGateState.PASS;
    }

    public boolean isFailed() {
        return gateState == ReleaseGateState.FAIL;
    }

    public boolean isPending() {
        return gateState == ReleaseGateState.PENDING;
    }

    /** 标记放行（自动判定通过或人工放行共用） */
    public void pass(String decidedBy, String reportRef) {
        this.gateState = ReleaseGateState.PASS;
        this.decidedBy = decidedBy;
        this.reportRef = reportRef;
        this.decidedAt = Instant.now();
    }

    /** 标记拦截 */
    public void fail(String decidedBy, String reportRef) {
        this.gateState = ReleaseGateState.FAIL;
        this.decidedBy = decidedBy;
        this.reportRef = reportRef;
        this.decidedAt = Instant.now();
    }

    /** 人工放行（override）：必须携带审批引用与决策人，并写不可变审计 */
    public void override(String decidedBy, String approvalRef, String reason) {
        this.override = true;
        this.gateState = ReleaseGateState.PASS;
        this.decidedBy = decidedBy;
        this.approvalRef = approvalRef;
        this.decidedAt = Instant.now();
        this.description = reason;
    }
}
