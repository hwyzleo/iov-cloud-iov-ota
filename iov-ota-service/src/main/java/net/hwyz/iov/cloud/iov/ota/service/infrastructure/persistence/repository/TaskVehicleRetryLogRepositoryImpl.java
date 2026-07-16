package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRetryLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleRetryLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 重试/续传轨迹审计仓储实现
 */
@Repository
@RequiredArgsConstructor
public class TaskVehicleRetryLogRepositoryImpl implements TaskVehicleRetryLogRepository {
    
    private final TaskVehicleRetryLogMapper mapper;
    
    @Override
    public Optional<TaskVehicleRetryLog> getById(Long id) {
        TaskVehicleRetryLogPo po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<TaskVehicleRetryLog> listByTaskId(Long taskId) {
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("row_valid", 1).orderByDesc("retried_at");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaskVehicleRetryLog> listByTaskIdAndVin(Long taskId, String vin) {
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("vin", vin).eq("row_valid", 1).orderByDesc("retried_at");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaskVehicleRetryLog> listByTaskIdAndVinAndStage(Long taskId, String vin, String stage) {
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId).eq("vin", vin).eq("stage", stage).eq("row_valid", 1).orderByDesc("retried_at");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public TaskVehicleRetryLog save(TaskVehicleRetryLog entity) {
        TaskVehicleRetryLogPo po = toPo(entity);
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
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId);
        mapper.delete(query);
    }
    
    private TaskVehicleRetryLog toDomain(TaskVehicleRetryLogPo po) {
        if (po == null) {
            return null;
        }
        return TaskVehicleRetryLog.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .vin(po.getVin())
                .stage(po.getStage())
                .attemptNo(po.getAttemptNo())
                .offset(po.getOffset())
                .result(po.getResult())
                .reason(po.getReason())
                .retriedAt(po.getRetriedAt() != null ? po.getRetriedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .description(po.getDescription())
                .build();
    }
    
    private TaskVehicleRetryLogPo toPo(TaskVehicleRetryLog domain) {
        if (domain == null) {
            return null;
        }
        return TaskVehicleRetryLogPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .vin(domain.getVin())
                .stage(domain.getStage())
                .attemptNo(domain.getAttemptNo())
                .offset(domain.getOffset())
                .result(domain.getResult())
                .reason(domain.getReason())
                .retriedAt(domain.getRetriedAt() != null ? LocalDateTime.ofInstant(domain.getRetriedAt(), ZoneId.systemDefault()) : null)
                .description(domain.getDescription())
                .build();
    }
}
