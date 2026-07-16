package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStateLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskStateLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStateLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务状态迁移审计仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TaskStateLogRepositoryImpl implements TaskStateLogRepository {
    
    private final TaskStateLogMapper mapper;
    
    @Override
    public Optional<TaskStateLog> getById(Long id) {
        TaskStateLogPo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<TaskStateLog> listByTaskId(Long taskId) {
        QueryWrapper<TaskStateLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("row_valid", 1).orderByDesc("decided_at");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaskStateLog> listByTaskIdAndAction(Long taskId, String action) {
        QueryWrapper<TaskStateLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("action", action).eq("row_valid", 1).orderByDesc("decided_at");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public TaskStateLog save(TaskStateLog entity) {
        TaskStateLogPo po = toPo(entity);
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
    
    @Override
    public void deleteByTaskId(Long taskId) {
        QueryWrapper<TaskStateLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId);
        mapper.delete(query);
    }
    
    private TaskStateLog toDomain(TaskStateLogPo po) {
        if (po == null) {
            return null;
        }
        return TaskStateLog.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .fromState(po.getFromState())
                .toState(po.getToState())
                .action(po.getAction())
                .operator(po.getOperator())
                .reason(po.getReason())
                .decidedAt(po.getDecidedAt() != null ? po.getDecidedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .description(po.getDescription())
                .build();
    }
    
    private TaskStateLogPo toPo(TaskStateLog domain) {
        if (domain == null) {
            return null;
        }
        return TaskStateLogPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .fromState(domain.getFromState())
                .toState(domain.getToState())
                .action(domain.getAction())
                .operator(domain.getOperator())
                .reason(domain.getReason())
                .decidedAt(domain.getDecidedAt() != null ? LocalDateTime.ofInstant(domain.getDecidedAt(), ZoneId.systemDefault()) : null)
                .description(domain.getDescription())
                .build();
    }
}
