package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskBatch;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskBatchRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskBatchMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskBatchPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TaskBatch Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskBatchRepositoryImpl implements TaskBatchRepository {

    private final TaskBatchMapper mapper;

    @Override
    public Optional<TaskBatch> getById(Long id) {
        TaskBatchPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public TaskBatch save(TaskBatch entity) {
        TaskBatchPo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TaskBatch toDomain(TaskBatchPo po) {
        TaskBatch domain = new TaskBatch();
        domain.setId(po.getId());
        return domain;
    }

    private TaskBatchPo toPo(TaskBatch domain) {
        TaskBatchPo po = new TaskBatchPo();
        po.setId(domain.getId());
        return po;
    }
}
