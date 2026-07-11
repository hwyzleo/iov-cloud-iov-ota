package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskMetric;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskMetricRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskMetricMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskMetricPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TaskMetric Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskMetricRepositoryImpl implements TaskMetricRepository {

    private final TaskMetricMapper mapper;

    @Override
    public Optional<TaskMetric> getById(Long id) {
        TaskMetricPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public TaskMetric save(TaskMetric entity) {
        TaskMetricPo po = toPo(entity);
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

    private TaskMetric toDomain(TaskMetricPo po) {
        TaskMetric domain = new TaskMetric();
        domain.setId(po.getId());
        return domain;
    }

    private TaskMetricPo toPo(TaskMetric domain) {
        TaskMetricPo po = new TaskMetricPo();
        po.setId(domain.getId());
        return po;
    }
}
