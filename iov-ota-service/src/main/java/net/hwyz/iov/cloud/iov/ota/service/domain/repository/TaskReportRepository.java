package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;

import java.util.Optional;

/**
 * TaskReport 仓储接口
 *
 * @author hwyz_leo
 */
public interface TaskReportRepository {

    Optional<TaskReport> getById(Long id);

    TaskReport save(TaskReport entity);

    void deleteById(Long id);
}
