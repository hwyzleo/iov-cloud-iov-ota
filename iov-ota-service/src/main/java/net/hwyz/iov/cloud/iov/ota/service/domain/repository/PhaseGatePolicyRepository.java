package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;

import java.util.List;
import java.util.Optional;

/**
 * 阶段门禁阈值策略仓储接口
 * <p>查询优先级：活动级覆盖（activity_id 非空）优先，其次全局策略（activity_id 为空）。</p>
 *
 * @author hwyz_leo
 */
public interface PhaseGatePolicyRepository {

    Optional<PhaseGatePolicy> getById(Long id);

    Optional<PhaseGatePolicy> findByPhaseAndActivity(Integer phaseValue, Long activityId);

    List<PhaseGatePolicy> listByPhase(Integer phaseValue);

    PhaseGatePolicy save(PhaseGatePolicy entity);

    void deleteById(Long id);
}
