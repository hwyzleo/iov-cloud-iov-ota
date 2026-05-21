package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskDo;

/**
 * 升级任务领域仓库接口
 * Phase 1-2: 暂时使用TaskDo，Phase 5将切换为Task聚合根
 *
 * @author hwyz_leo
 */
public interface TaskRepository extends BaseRepository<Long, TaskDo> {
}
