package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition;

import java.util.List;
import java.util.Optional;

/**
 * 任务安装条件仓储接口
 */
public interface TaskInstallConditionRepository {
    
    Optional<TaskInstallCondition> getById(Long id);
    
    List<TaskInstallCondition> listByTaskId(Long taskId);
    
    List<TaskInstallCondition> listByTaskIdAndType(Long taskId, String conditionType);
    
    TaskInstallCondition save(TaskInstallCondition entity);
    
    void deleteById(Long id);
    
    void deleteByTaskId(Long taskId);
}
