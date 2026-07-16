package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStateLog;

import java.util.List;
import java.util.Optional;

/**
 * 任务状态迁移审计仓储接口
 */
public interface TaskStateLogRepository {
    
    Optional<TaskStateLog> getById(Long id);
    
    List<TaskStateLog> listByTaskId(Long taskId);
    
    List<TaskStateLog> listByTaskIdAndAction(Long taskId, String action);
    
    TaskStateLog save(TaskStateLog entity);
    
    void deleteById(Long id);
    
    void deleteByTaskId(Long taskId);
}
