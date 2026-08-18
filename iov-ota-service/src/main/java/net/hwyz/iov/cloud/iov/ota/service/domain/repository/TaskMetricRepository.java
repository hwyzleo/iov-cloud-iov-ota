package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskMetric;

import java.util.List;
import java.util.Optional;

/**
 * TaskMetric 仓储接口（CR-015：Task 级统计）
 *
 * @author hwyz_leo
 */
public interface TaskMetricRepository {

    Optional<TaskMetric> getById(Long id);

    Optional<TaskMetric> findLatestByTaskId(Long taskId);

    List<TaskMetric> listByTaskId(Long taskId, int limit);

    TaskMetric save(TaskMetric entity);

    void deleteById(Long id);
}
