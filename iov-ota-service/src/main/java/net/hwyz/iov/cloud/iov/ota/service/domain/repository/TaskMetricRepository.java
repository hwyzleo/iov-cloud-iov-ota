package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskMetric;

import java.util.Optional;

/**
 * TaskMetric 仓储接口
 *
 * @author hwyz_leo
 */
public interface TaskMetricRepository {

    Optional<TaskMetric> getById(Long id);

    TaskMetric save(TaskMetric entity);

    void deleteById(Long id);
}
