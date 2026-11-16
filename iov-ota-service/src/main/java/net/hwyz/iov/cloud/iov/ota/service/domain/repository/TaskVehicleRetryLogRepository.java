package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 重试/续传轨迹审计仓储接口
 */
public interface TaskVehicleRetryLogRepository {
    
    Optional<TaskVehicleRetryLog> getById(Long id);
    
    List<TaskVehicleRetryLog> listByTaskId(Long taskId);
    
    List<TaskVehicleRetryLog> listByTaskIdAndVin(Long taskId, String vin);
    
    List<TaskVehicleRetryLog> listByTaskIdAndVinAndStage(Long taskId, String vin, String stage);
    
    /**
     * 按业务幂等键读取重试事实（CR-026）。
     */
    Optional<TaskVehicleRetryLog> getByRetryEventId(String retryEventId);

    /**
     * 按业务消息身份幂等追加不可变重试事实（CR-026 §7.4）。
     *
     * <p>同一 retryEventId 且事实一致时返回既有行（技术重投不追加）；事实不一致抛冲突。
     */
    TaskVehicleRetryLog appendIdempotent(TaskVehicleRetryLog entity);

    /**
     * 按车辆任务维度分页查询（过滤交由 SQL，CR-026 §8.2）。
     */
    List<TaskVehicleRetryLog> listByVehicleTask(Long vehicleTaskId, Instant beginTime, Instant endTime,
                                               String stage, String action, Integer attemptNo);

    TaskVehicleRetryLog save(TaskVehicleRetryLog entity);
    
    void deleteById(Long id);
    
    void deleteByTaskId(Long taskId);
}
