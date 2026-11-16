package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.RetryTrajectoryException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRetryLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleRetryLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
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
    public Optional<TaskVehicleRetryLog> getByRetryEventId(String retryEventId) {
        if (retryEventId == null || retryEventId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(toDomain(mapper.selectByRetryEventId(retryEventId)));
    }

    @Override
    public TaskVehicleRetryLog appendIdempotent(TaskVehicleRetryLog entity) {
        Optional<TaskVehicleRetryLog> existing = getByRetryEventId(entity.getRetryEventId());
        if (existing.isPresent()) {
            return reuseOrConflict(existing.get(), entity);
        }
        mapper.insert(toPo(entity));
        return entity;
    }

    @Override
    public List<TaskVehicleRetryLog> listByVehicleTask(Long vehicleTaskId, Instant beginTime, Instant endTime,
                                                       String stage, String action, Integer attemptNo) {
        QueryWrapper<TaskVehicleRetryLogPo> query = new QueryWrapper<>();
        query.eq("vehicle_task_id", vehicleTaskId).eq("row_valid", 1);
        if (beginTime != null) {
            query.ge("retried_at", LocalDateTime.ofInstant(beginTime, ZoneId.systemDefault()));
        }
        if (endTime != null) {
            query.le("retried_at", LocalDateTime.ofInstant(endTime, ZoneId.systemDefault()));
        }
        if (stage != null && !stage.isBlank()) {
            query.eq("stage", stage);
        }
        if (action != null && !action.isBlank()) {
            query.eq("action", action);
        }
        if (attemptNo != null) {
            query.eq("attempt_no", attemptNo);
        }
        query.orderByDesc("retried_at").orderByDesc("id");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 同一 retryEventId 的幂等复用判定：事实一致返回既有行，否则确定性冲突（403015）。
     */
    private TaskVehicleRetryLog reuseOrConflict(TaskVehicleRetryLog existing, TaskVehicleRetryLog incoming) {
        boolean sameFact = Objects.equals(existing.getStage(), incoming.getStage())
                && Objects.equals(existing.getAction(), incoming.getAction())
                && Objects.equals(existing.getAttemptNo(), incoming.getAttemptNo())
                && Objects.equals(existing.getOffset(), incoming.getOffset())
                && Objects.equals(existing.getResult(), incoming.getResult());
        if (!sameFact) {
            throw new RetryTrajectoryException("同一 retryEventId[" + incoming.getRetryEventId()
                    + "]对应不同重试/续传事实");
        }
        return existing;
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
                .retryEventId(po.getRetryEventId())
                .vehicleTaskId(po.getVehicleTaskId())
                .stage(po.getStage())
                .action(po.getAction())
                .errorCode(po.getErrorCode())
                .attemptNo(po.getAttemptNo())
                .offset(po.getOffset())
                .result(po.getResult())
                .reason(po.getReason())
                .retriedAt(po.getRetriedAt() != null ? po.getRetriedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .sourceOccurredAt(po.getSourceOccurredAt() != null
                        ? po.getSourceOccurredAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .receivedAt(po.getReceivedAt() != null
                        ? po.getReceivedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
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
                .retryEventId(domain.getRetryEventId())
                .vehicleTaskId(domain.getVehicleTaskId())
                .stage(domain.getStage())
                .action(domain.getAction())
                .errorCode(domain.getErrorCode())
                .attemptNo(domain.getAttemptNo())
                .offset(domain.getOffset())
                .result(domain.getResult())
                .reason(domain.getReason())
                .retriedAt(domain.getRetriedAt() != null ? LocalDateTime.ofInstant(domain.getRetriedAt(), ZoneId.systemDefault()) : null)
                .sourceOccurredAt(domain.getSourceOccurredAt() != null
                        ? LocalDateTime.ofInstant(domain.getSourceOccurredAt(), ZoneId.systemDefault()) : null)
                .receivedAt(domain.getReceivedAt() != null
                        ? LocalDateTime.ofInstant(domain.getReceivedAt(), ZoneId.systemDefault()) : null)
                .description(domain.getDescription())
                .createTime(domain.getRetriedAt() == null ? new Date() : new Date(domain.getRetriedAt().toEpochMilli()))
                .modifyTime(new Date())
                .rowValid(true)
                .build();
    }
}
