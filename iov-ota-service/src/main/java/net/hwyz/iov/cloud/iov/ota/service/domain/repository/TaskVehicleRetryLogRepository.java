package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;

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
    
    TaskVehicleRetryLog save(TaskVehicleRetryLog entity);
    
    void deleteById(Long id);
    
    void deleteByTaskId(Long taskId);
}
