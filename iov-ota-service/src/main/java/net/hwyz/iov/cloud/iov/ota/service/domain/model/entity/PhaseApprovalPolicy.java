package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 按阶段审批要求领域实体
 * 对应表：tb_phase_approval_policy
 */
@Getter
@Setter
@Builder
public class PhaseApprovalPolicy {
    
    private Long id;
    private TaskPhase phase;
    private Long activityId;  // 活动级覆盖（可空，空为全局策略）
    private Boolean required;  // 是否需要审批
    private String requiredLevels;  // 需要审批级别（逗号分隔）
    
    /**
     * 获取需要的审批级别列表
     */
    public List<ApprovalLevel> getRequiredLevelList() {
        if (requiredLevels == null || requiredLevels.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(requiredLevels.split(","))
                .map(String::trim)
                .map(ApprovalLevel::valueOf)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查指定级别是否需要审批
     */
    public boolean isLevelRequired(ApprovalLevel level) {
        return getRequiredLevelList().contains(level);
    }
    
    /**
     * 获取下一个需要的审批级别
     * @param currentLevel 当前级别
     * @return 下一个级别，如果没有则返回null
     */
    public ApprovalLevel getNextRequiredLevel(ApprovalLevel currentLevel) {
        List<ApprovalLevel> levels = getRequiredLevelList();
        int currentIndex = levels.indexOf(currentLevel);
        if (currentIndex < 0 || currentIndex >= levels.size() - 1) {
            return null;
        }
        return levels.get(currentIndex + 1);
    }
    
    /**
     * 检查是否所有级别都已审批通过
     * @param approvals 已完成的审批列表
     * @return 是否全部通过
     */
    public boolean areAllLevelsApproved(List<TaskApproval> approvals) {
        List<ApprovalLevel> requiredLevels = getRequiredLevelList();
        for (ApprovalLevel level : requiredLevels) {
            boolean approved = approvals.stream()
                    .anyMatch(a -> a.getLevel() == level && a.isApproved());
            if (!approved) {
                return false;
            }
        }
        return true;
    }
}