package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.GateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskPhaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskPhaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskPhaseGateMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPhaseGatePo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 跨任务阶段门禁仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TaskPhaseGateRepositoryImpl implements TaskPhaseGateRepository {
    
    private final TaskPhaseGateMapper mapper;
    
    @Override
    public Optional<TaskPhaseGate> getById(Long id) {
        TaskPhaseGatePo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public Optional<TaskPhaseGate> getByActivityIdAndToPhase(Long activityId, TaskPhase toPhase) {
        QueryWrapper<TaskPhaseGatePo> query = new QueryWrapper<>();
        query.eq("activity_id", activityId)
             .eq("to_phase", toPhase.getValue())
             .eq("row_valid", 1);
        TaskPhaseGatePo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<TaskPhaseGate> listByActivityId(Long activityId) {
        QueryWrapper<TaskPhaseGatePo> query = new QueryWrapper<>();
        query.eq("activity_id", activityId)
             .eq("row_valid", 1)
             .orderByAsc("to_phase");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public TaskPhaseGate save(TaskPhaseGate entity) {
        TaskPhaseGatePo po = toPo(entity);
        if (po.getId() == null) {
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
    
    private TaskPhaseGate toDomain(TaskPhaseGatePo po) {
        if (po == null) {
            return null;
        }
        return TaskPhaseGate.builder()
                .id(po.getId())
                .activityId(po.getActivityId())
                .fromPhase(TaskPhase.valOf(po.getFromPhase()))
                .toPhase(TaskPhase.valOf(po.getToPhase()))
                .prevTaskId(po.getPrevTaskId())
                .gateState(po.getGateState() != null ? GateState.valueOf(po.getGateState()) : null)
                .override(po.getOverride())
                .approvalRef(po.getApprovalRef())
                .decidedBy(po.getDecidedBy())
                .decidedAt(po.getDecidedAt() != null ? po.getDecidedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .reportRef(po.getReportRef())
                .gateThresholdSnapshot(po.getGateThresholdSnapshot())
                .build();
    }
    
    private TaskPhaseGatePo toPo(TaskPhaseGate domain) {
        if (domain == null) {
            return null;
        }
        return TaskPhaseGatePo.builder()
                .id(domain.getId())
                .activityId(domain.getActivityId())
                .fromPhase(domain.getFromPhase() != null ? domain.getFromPhase().getValue() : null)
                .toPhase(domain.getToPhase() != null ? domain.getToPhase().getValue() : null)
                .prevTaskId(domain.getPrevTaskId())
                .gateState(domain.getGateState() != null ? domain.getGateState().name() : null)
                .override(domain.getOverride())
                .approvalRef(domain.getApprovalRef())
                .decidedBy(domain.getDecidedBy())
                .decidedAt(domain.getDecidedAt() != null ? LocalDateTime.ofInstant(domain.getDecidedAt(), java.time.ZoneId.systemDefault()) : null)
                .reportRef(domain.getReportRef())
                .gateThresholdSnapshot(domain.getGateThresholdSnapshot())
                .build();
    }
}