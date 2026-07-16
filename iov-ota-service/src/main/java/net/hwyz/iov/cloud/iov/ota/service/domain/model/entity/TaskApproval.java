package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;

import java.time.Instant;

/**
 * 任务审批链留痕领域实体
 * 对应表：tb_task_approval
 */
@Getter
@Setter
@Builder
public class TaskApproval {
    
    private Long id;
    private Long taskId;
    private ApprovalLevel level;  // QUALITY/PRODUCT/SECURITY
    private String approver;
    private String result;  // APPROVED/REJECTED
    private String comment;
    private Instant decidedAt;
    private String approvalRef;  // 审批引用（跳阶授权时）
    
    /**
     * 判断是否通过
     */
    public boolean isApproved() {
        return "APPROVED".equals(result);
    }
    
    /**
     * 判断是否驳回
     */
    public boolean isRejected() {
        return "REJECTED".equals(result);
    }
    
    /**
     * 创建审批记录
     */
    public static TaskApproval approve(Long taskId, ApprovalLevel level, String approver, String comment) {
        return TaskApproval.builder()
                .taskId(taskId)
                .level(level)
                .approver(approver)
                .result("APPROVED")
                .comment(comment)
                .decidedAt(Instant.now())
                .build();
    }
    
    /**
     * 创建驳回记录
     */
    public static TaskApproval reject(Long taskId, ApprovalLevel level, String approver, String comment) {
        return TaskApproval.builder()
                .taskId(taskId)
                .level(level)
                .approver(approver)
                .result("REJECTED")
                .comment(comment)
                .decidedAt(Instant.now())
                .build();
    }
}