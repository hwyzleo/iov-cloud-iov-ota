package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;

import java.util.List;
import java.util.Optional;

/**
 * 任务审批仓储接口
 */
public interface TaskApprovalRepository {
    
    Optional<TaskApproval> getById(Long id);
    
    List<TaskApproval> listByTaskId(Long taskId);
    
    TaskApproval save(TaskApproval entity);
    
    void deleteById(Long id);
}