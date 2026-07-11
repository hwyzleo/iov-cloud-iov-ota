package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskReportMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReportPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TaskReport Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskReportRepositoryImpl implements TaskReportRepository {

    private final TaskReportMapper mapper;

    @Override
    public Optional<TaskReport> getById(Long id) {
        TaskReportPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public TaskReport save(TaskReport entity) {
        TaskReportPo po = toPo(entity);
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

    private TaskReport toDomain(TaskReportPo po) {
        TaskReport domain = new TaskReport();
        domain.setId(po.getId());
        return domain;
    }

    private TaskReportPo toPo(TaskReport domain) {
        TaskReportPo po = new TaskReportPo();
        po.setId(domain.getId());
        return po;
    }
}
