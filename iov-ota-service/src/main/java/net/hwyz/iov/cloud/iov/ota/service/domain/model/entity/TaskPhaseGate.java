package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.GateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;

import java.time.Instant;

/**
 * 跨任务阶段门禁领域实体
 * 对应表：tb_task_phase_gate
 */
@Getter
@Setter
@Builder
public class TaskPhaseGate {
    
    private Long id;
    private Long activityId;
    private TaskPhase fromPhase;
    private TaskPhase toPhase;
    private Long prevTaskId;
    private GateState gateState;
    private Boolean override;  // 是否人工跳阶授权
    private String approvalRef;
    private String decidedBy;
    private Instant decidedAt;
    private String reportRef;
    private String gateThresholdSnapshot;  // JSON格式
    
    /**
     * 判断门禁是否通过
     */
    public boolean isPassed() {
        return gateState == GateState.OK;
    }
    
    /**
     * 判断门禁是否失败
     */
    public boolean isFailed() {
        return gateState == GateState.BREACH;
    }
    
    /**
     * 判断门禁是否待定
     */
    public boolean isPending() {
        return gateState == null;
    }
    
    /**
     * 设置门禁状态为通过
     */
    public void pass(String decidedBy, String reportRef) {
        this.gateState = GateState.OK;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.reportRef = reportRef;
    }
    
    /**
     * 设置门禁状态为失败
     */
    public void fail(String decidedBy, String reportRef) {
        this.gateState = GateState.BREACH;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.reportRef = reportRef;
    }
    
    /**
     * 人工跳阶授权
     */
    public void override(String decidedBy, String approvalRef) {
        this.override = true;
        this.gateState = GateState.OK;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.approvalRef = approvalRef;
    }
}