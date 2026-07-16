package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskPhaseGate;

import java.util.List;
import java.util.Optional;

/**
 * 跨任务阶段门禁仓储接口
 */
public interface TaskPhaseGateRepository {
    
    Optional<TaskPhaseGate> getById(Long id);
    
    Optional<TaskPhaseGate> getByActivityIdAndToPhase(Long activityId, TaskPhase toPhase);
    
    List<TaskPhaseGate> listByActivityId(Long activityId);
    
    TaskPhaseGate save(TaskPhaseGate entity);
    
    void deleteById(Long id);
}