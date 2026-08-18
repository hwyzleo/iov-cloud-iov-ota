package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskReleaseGateMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReleaseGatePo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 多任务放行门禁仓储实现（CR-015）
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class TaskReleaseGateRepositoryImpl implements TaskReleaseGateRepository {

    private final TaskReleaseGateMapper mapper;

    @Override
    public Optional<TaskReleaseGate> getById(Long id) {
        TaskReleaseGatePo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TaskReleaseGate> getByNextTaskId(Long nextTaskId) {
        QueryWrapper<TaskReleaseGatePo> query = new QueryWrapper<>();
        query.eq("next_task_id", nextTaskId)
             .eq("row_valid", 1);
        TaskReleaseGatePo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TaskReleaseGate> getByPreviousTaskId(Long previousTaskId) {
        QueryWrapper<TaskReleaseGatePo> query = new QueryWrapper<>();
        query.eq("previous_task_id", previousTaskId)
             .eq("row_valid", 1)
             .orderByDesc("id")
             .last("LIMIT 1");
        TaskReleaseGatePo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TaskReleaseGate> listByPreviousTaskIdAndGateState(Long previousTaskId, String gateState) {
        QueryWrapper<TaskReleaseGatePo> query = new QueryWrapper<>();
        query.eq("previous_task_id", previousTaskId)
             .eq("gate_state", gateState)
             .eq("row_valid", 1)
             .orderByDesc("id");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskReleaseGate> listByActivityId(Long activityId) {
        QueryWrapper<TaskReleaseGatePo> query = new QueryWrapper<>();
        query.eq("activity_id", activityId)
             .eq("row_valid", 1)
             .orderByDesc("id");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TaskReleaseGate save(TaskReleaseGate entity) {
        TaskReleaseGatePo po = toPo(entity);
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

    private TaskReleaseGate toDomain(TaskReleaseGatePo po) {
        if (po == null) {
            return null;
        }
        return TaskReleaseGate.builder()
                .id(po.getId())
                .activityId(po.getActivityId())
                .previousTaskId(po.getPreviousTaskId())
                .nextTaskId(po.getNextTaskId())
                .gateType(po.getGateType() != null ? TaskReleaseGate.ReleaseGateType.valueOf(po.getGateType()) : null)
                .gateState(po.getGateState() != null ? ReleaseGateState.valOf(po.getGateState()) : ReleaseGateState.PENDING)
                .gateThresholdSnapshot(po.getGateThresholdSnapshot())
                .reportRef(po.getReportRef())
                .override(po.getOverride())
                .approvalRef(po.getApprovalRef())
                .decidedBy(po.getDecidedBy())
                .decidedAt(po.getDecidedAt() != null ? po.getDecidedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .description(po.getDescription())
                .build();
    }

    private TaskReleaseGatePo toPo(TaskReleaseGate domain) {
        if (domain == null) {
            return null;
        }
        return TaskReleaseGatePo.builder()
                .id(domain.getId())
                .activityId(domain.getActivityId())
                .previousTaskId(domain.getPreviousTaskId())
                .nextTaskId(domain.getNextTaskId())
                .gateType(domain.getGateType() != null ? domain.getGateType().name() : null)
                .gateState(domain.getGateState() != null ? domain.getGateState().getValue() : ReleaseGateState.PENDING.getValue())
                .gateThresholdSnapshot(domain.getGateThresholdSnapshot())
                .reportRef(domain.getReportRef())
                .override(domain.getOverride())
                .approvalRef(domain.getApprovalRef())
                .decidedBy(domain.getDecidedBy())
                .decidedAt(domain.getDecidedAt() != null ? LocalDateTime.ofInstant(domain.getDecidedAt(), java.time.ZoneId.systemDefault()) : null)
                .description(domain.getDescription())
                .build();
    }
}
