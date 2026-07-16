package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseApprovalPolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskApprovalRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseApprovalPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 审批领域服务
 * 实现 US-060：审批与阶段结合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalDomainService {
    
    private final TaskApprovalRepository taskApprovalRepository;
    private final PhaseApprovalPolicyRepository phaseApprovalPolicyRepository;
    
    /**
     * 检查任务是否满足审批要求
     * @param taskId 任务ID
     * @param phase 任务阶段
     * @param activityId 活动ID
     * @return 是否满足审批要求
     */
    public boolean checkApprovalRequirements(Long taskId, TaskPhase phase, Long activityId) {
        // 获取审批策略
        Optional<PhaseApprovalPolicy> policyOpt = phaseApprovalPolicyRepository.getByPhaseAndActivityId(phase, activityId);
        if (policyOpt.isEmpty()) {
            // 没有审批策略，默认不需要审批
            log.debug("阶段[{}]活动[{}]没有审批策略，默认通过", phase, activityId);
            return true;
        }
        
        PhaseApprovalPolicy policy = policyOpt.get();
        if (!policy.getRequired()) {
            // 不需要审批
            log.debug("阶段[{}]活动[{}]不需要审批", phase, activityId);
            return true;
        }
        
        // 获取任务的审批记录
        List<TaskApproval> approvals = taskApprovalRepository.listByTaskId(taskId);
        
        // 检查是否所有级别都已审批通过
        boolean allApproved = policy.areAllLevelsApproved(approvals);
        
        log.info("任务[{}]阶段[{}]审批检查结果[{}]", taskId, phase, allApproved ? "通过" : "不通过");
        return allApproved;
    }
    
    /**
     * 获取下一个需要的审批级别
     * @param taskId 任务ID
     * @param phase 任务阶段
     * @param activityId 活动ID
     * @return 下一个审批级别，如果不需要审批或已全部完成则返回null
     */
    public ApprovalLevel getNextApprovalLevel(Long taskId, TaskPhase phase, Long activityId) {
        Optional<PhaseApprovalPolicy> policyOpt = phaseApprovalPolicyRepository.getByPhaseAndActivityId(phase, activityId);
        if (policyOpt.isEmpty() || !policyOpt.get().getRequired()) {
            return null;
        }
        
        PhaseApprovalPolicy policy = policyOpt.get();
        List<TaskApproval> approvals = taskApprovalRepository.listByTaskId(taskId);
        
        // 找到当前最高审批级别
        ApprovalLevel currentLevel = approvals.stream()
                .filter(TaskApproval::isApproved)
                .map(TaskApproval::getLevel)
                .max(Enum::compareTo)
                .orElse(null);
        
        return policy.getNextRequiredLevel(currentLevel);
    }
    
    /**
     * 提交审批
     * @param taskId 任务ID
     * @param level 审批级别
     * @param approver 审批人
     * @param result 审批结果（APPROVED/REJECTED）
     * @param comment 审批意见
     * @return 审批记录
     */
    public TaskApproval submitApproval(Long taskId, ApprovalLevel level, String approver, 
                                      String result, String comment) {
        TaskApproval approval;
        if ("APPROVED".equals(result)) {
            approval = TaskApproval.approve(taskId, level, approver, comment);
        } else {
            approval = TaskApproval.reject(taskId, level, approver, comment);
        }
        
        approval = taskApprovalRepository.save(approval);
        log.info("任务[{}]审批提交：级别[{}]，结果[{}]，审批人[{}]", taskId, level, result, approver);
        
        return approval;
    }
    
    /**
     * 获取任务的所有审批记录
     * @param taskId 任务ID
     * @return 审批记录列表
     */
    public List<TaskApproval> listApprovals(Long taskId) {
        return taskApprovalRepository.listByTaskId(taskId);
    }
    
    /**
     * 检查是否可以跳过审批（人工跳阶授权）
     * @param taskId 任务ID
     * @param phase 任务阶段
     * @param activityId 活动ID
     * @param approvalRef 审批引用
     * @return 是否可以跳过
     */
    public boolean canSkipApproval(Long taskId, TaskPhase phase, Long activityId, String approvalRef) {
        // 检查是否有跳阶授权审批
        List<TaskApproval> approvals = taskApprovalRepository.listByTaskId(taskId);
        return approvals.stream()
                .anyMatch(a -> approvalRef.equals(a.getApprovalRef()) && a.isApproved());
    }
}