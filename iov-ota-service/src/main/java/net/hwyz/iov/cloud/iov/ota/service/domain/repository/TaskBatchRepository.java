package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskBatch;

import java.util.Optional;

/**
 * TaskBatch 仓储接口
 *
 * @author hwyz_leo
 */
public interface TaskBatchRepository {

    Optional<TaskBatch> getById(Long id);

    TaskBatch save(TaskBatch entity);

    void deleteById(Long id);
}
