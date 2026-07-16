package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskInstallConditionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskInstallConditionPo;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务安装条件仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TaskInstallConditionRepositoryImpl implements TaskInstallConditionRepository {
    
    private final TaskInstallConditionMapper mapper;
    
    @Override
    public Optional<TaskInstallCondition> getById(Long id) {
        TaskInstallConditionPo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<TaskInstallCondition> listByTaskId(Long taskId) {
        QueryWrapper<TaskInstallConditionPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("row_valid", 1).orderByAsc("condition_type");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaskInstallCondition> listByTaskIdAndType(Long taskId, String conditionType) {
        QueryWrapper<TaskInstallConditionPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("condition_type", conditionType).eq("row_valid", 1);
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public TaskInstallCondition save(TaskInstallCondition entity) {
        TaskInstallConditionPo po = toPo(entity);
        if (po.getId() == null) {
            po.setCreateTime(new Date());
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
        QueryWrapper<TaskInstallConditionPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId);
        mapper.delete(query);
    }
    
    private TaskInstallCondition toDomain(TaskInstallConditionPo po) {
        if (po == null) {
            return null;
        }
        return TaskInstallCondition.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .conditionType(po.getConditionType())
                .operator(po.getOperator())
                .threshold(po.getThreshold())
                .severity(po.getSeverity())
                .description(po.getDescription())
                .build();
    }
    
    private TaskInstallConditionPo toPo(TaskInstallCondition domain) {
        if (domain == null) {
            return null;
        }
        return TaskInstallConditionPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .conditionType(domain.getConditionType())
                .operator(domain.getOperator())
                .threshold(domain.getThreshold())
                .severity(domain.getSeverity())
                .description(domain.getDescription())
                .build();
    }
}
