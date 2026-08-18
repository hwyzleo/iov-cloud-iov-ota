package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;

import java.util.List;
import java.util.Optional;

/**
 * TaskReport 仓储接口（CR-015：正式报告幂等）
 *
 * @author hwyz_leo
 */
public interface TaskReportRepository {

    Optional<TaskReport> getById(Long id);

    /** 查询任务最新正式报告（按 report_version 降序） */
    Optional<TaskReport> findLatestByTaskId(Long taskId);

    List<TaskReport> listByTaskId(Long taskId);

    TaskReport save(TaskReport entity);

    void deleteById(Long id);
}
