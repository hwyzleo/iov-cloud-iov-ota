package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseApprovalPolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseApprovalPolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PhaseApprovalPolicyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PhaseApprovalPolicyPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 阶段审批策略仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PhaseApprovalPolicyRepositoryImpl implements PhaseApprovalPolicyRepository {
    
    private final PhaseApprovalPolicyMapper mapper;
    
    @Override
    public Optional<PhaseApprovalPolicy> getByPhaseAndActivityId(TaskPhase phase, Long activityId) {
        QueryWrapper<PhaseApprovalPolicyPo> query = new QueryWrapper<>();
        query.eq("phase", phase.getValue())
             .and(w -> w.eq("activity_id", activityId).or().isNull("activity_id"))
             .eq("row_valid", 1)
             .orderByDesc("activity_id")  // 活动级优先
             .last("LIMIT 1");
        PhaseApprovalPolicyPo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public PhaseApprovalPolicy save(PhaseApprovalPolicy entity) {
        PhaseApprovalPolicyPo po = toPo(entity);
        if (po.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }
    
    private PhaseApprovalPolicy toDomain(PhaseApprovalPolicyPo po) {
        if (po == null) {
            return null;
        }
        return PhaseApprovalPolicy.builder()
                .id(po.getId())
                .phase(TaskPhase.valOf(po.getPhase()))
                .activityId(po.getActivityId())
                .required(po.getRequired())
                .requiredLevels(po.getRequiredLevels())
                .build();
    }
    
    private PhaseApprovalPolicyPo toPo(PhaseApprovalPolicy domain) {
        if (domain == null) {
            return null;
        }
        return PhaseApprovalPolicyPo.builder()
                .id(domain.getId())
                .phase(domain.getPhase() != null ? domain.getPhase().getValue() : null)
                .activityId(domain.getActivityId())
                .required(domain.getRequired())
                .requiredLevels(domain.getRequiredLevels())
                .build();
    }
}