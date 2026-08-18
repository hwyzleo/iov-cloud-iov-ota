package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PhaseGatePolicyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PhaseGatePolicyPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 阶段门禁阈值策略仓储实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PhaseGatePolicyRepositoryImpl implements PhaseGatePolicyRepository {

    private final PhaseGatePolicyMapper mapper;

    @Override
    public Optional<PhaseGatePolicy> getById(Long id) {
        PhaseGatePolicyPo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<PhaseGatePolicy> findByPhaseAndActivity(Integer phaseValue, Long activityId) {
        // 查询活动级覆盖策略
        QueryWrapper<PhaseGatePolicyPo> activityQuery = new QueryWrapper<>();
        activityQuery.eq("phase", phaseValue)
                     .eq("activity_id", activityId)
                     .eq("row_valid", 1)
                     .orderByDesc("id")
                     .last("LIMIT 1");
        PhaseGatePolicyPo activityPo = mapper.selectOne(activityQuery);
        if (activityPo != null) {
            return Optional.of(toDomain(activityPo));
        }

        // 回退到全局策略（activity_id 为空）
        QueryWrapper<PhaseGatePolicyPo> globalQuery = new QueryWrapper<>();
        globalQuery.eq("phase", phaseValue)
                   .isNull("activity_id")
                   .eq("row_valid", 1)
                   .orderByDesc("id")
                   .last("LIMIT 1");
        PhaseGatePolicyPo globalPo = mapper.selectOne(globalQuery);
        return Optional.ofNullable(globalPo).map(this::toDomain);
    }

    @Override
    public List<PhaseGatePolicy> listByPhase(Integer phaseValue) {
        QueryWrapper<PhaseGatePolicyPo> query = new QueryWrapper<>();
        query.eq("phase", phaseValue)
             .eq("row_valid", 1)
             .orderByDesc("id");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PhaseGatePolicy save(PhaseGatePolicy entity) {
        PhaseGatePolicyPo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private PhaseGatePolicy toDomain(PhaseGatePolicyPo po) {
        return PhaseGatePolicy.builder()
                .id(po.getId())
                .phase(net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase.valOf(po.getPhase()))
                .activityId(po.getActivityId())
                .successRateMin(po.getSuccessRateMin())
                .failCntMax(po.getFailCntMax())
                .severeDefectAllowed(po.getSevereDefectAllowed())
                .build();
    }

    private PhaseGatePolicyPo toPo(PhaseGatePolicy domain) {
        return PhaseGatePolicyPo.builder()
                .id(domain.getId())
                .phase(domain.getPhase() != null ? domain.getPhase().getValue() : null)
                .activityId(domain.getActivityId())
                .successRateMin(domain.getSuccessRateMin())
                .failCntMax(domain.getFailCntMax())
                .severeDefectAllowed(domain.getSevereDefectAllowed())
                .build();
    }
}
