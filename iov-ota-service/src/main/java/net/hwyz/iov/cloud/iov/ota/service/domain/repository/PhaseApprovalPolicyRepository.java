package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseApprovalPolicy;

import java.util.Optional;

/**
 * 阶段审批策略仓储接口
 */
public interface PhaseApprovalPolicyRepository {
    
    Optional<PhaseApprovalPolicy> getByPhaseAndActivityId(TaskPhase phase, Long activityId);
    
    PhaseApprovalPolicy save(PhaseApprovalPolicy entity);
}