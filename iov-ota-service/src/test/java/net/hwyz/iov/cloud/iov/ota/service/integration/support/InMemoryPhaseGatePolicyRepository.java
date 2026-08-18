package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CR-015 集成测试内存版 PhaseGatePolicy 仓储
 *
 * @author hwyz_leo
 */
public class InMemoryPhaseGatePolicyRepository implements PhaseGatePolicyRepository {

    private final Map<Long, PhaseGatePolicy> store = new HashMap<>();
    private long seq = 1;

    @Override
    public Optional<PhaseGatePolicy> getById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<PhaseGatePolicy> findByPhaseAndActivity(Integer phaseValue, Long activityId) {
        // 活动级覆盖优先
        Optional<PhaseGatePolicy> activityPolicy = store.values().stream()
                .filter(p -> p.getPhase() != null && p.getPhase().getValue() == phaseValue)
                .filter(p -> activityId.equals(p.getActivityId()))
                .max(Comparator.comparing(PhaseGatePolicy::getId));
        if (activityPolicy.isPresent()) {
            return activityPolicy;
        }
        // 全局策略（activityId 为空）
        return store.values().stream()
                .filter(p -> p.getPhase() != null && p.getPhase().getValue() == phaseValue)
                .filter(p -> p.getActivityId() == null)
                .max(Comparator.comparing(PhaseGatePolicy::getId));
    }

    @Override
    public List<PhaseGatePolicy> listByPhase(Integer phaseValue) {
        return store.values().stream()
                .filter(p -> p.getPhase() != null && p.getPhase().getValue() == phaseValue)
                .collect(Collectors.toList());
    }

    @Override
    public PhaseGatePolicy save(PhaseGatePolicy entity) {
        if (entity.getId() == null) {
            entity.setId(seq++);
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
