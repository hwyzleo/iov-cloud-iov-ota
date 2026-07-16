package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskApprovalRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskApprovalMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskApprovalPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务审批仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TaskApprovalRepositoryImpl implements TaskApprovalRepository {
    
    private final TaskApprovalMapper mapper;
    
    @Override
    public Optional<TaskApproval> getById(Long id) {
        TaskApprovalPo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<TaskApproval> listByTaskId(Long taskId) {
        QueryWrapper<TaskApprovalPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1)
             .orderByAsc("level");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public TaskApproval save(TaskApproval entity) {
        TaskApprovalPo po = toPo(entity);
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
    
    private TaskApproval toDomain(TaskApprovalPo po) {
        if (po == null) {
            return null;
        }
        return TaskApproval.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .level(po.getLevel() != null ? ApprovalLevel.valueOf(po.getLevel()) : null)
                .approver(po.getApprover())
                .result(po.getResult())
                .comment(po.getComment())
                .decidedAt(po.getDecidedAt() != null ? po.getDecidedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .approvalRef(po.getApprovalRef())
                .build();
    }
    
    private TaskApprovalPo toPo(TaskApproval domain) {
        if (domain == null) {
            return null;
        }
        return TaskApprovalPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .level(domain.getLevel() != null ? domain.getLevel().name() : null)
                .approver(domain.getApprover())
                .result(domain.getResult())
                .comment(domain.getComment())
                .decidedAt(domain.getDecidedAt() != null ? LocalDateTime.ofInstant(domain.getDecidedAt(), java.time.ZoneId.systemDefault()) : null)
                .approvalRef(domain.getApprovalRef())
                .build();
    }
}