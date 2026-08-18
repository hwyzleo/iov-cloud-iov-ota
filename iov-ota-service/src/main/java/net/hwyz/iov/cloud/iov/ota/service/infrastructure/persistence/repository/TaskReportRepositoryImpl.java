package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskReportMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskReportPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TaskReport Repository实现（CR-015：正式报告 reportVersion 幂等）
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
    public Optional<TaskReport> findLatestByTaskId(Long taskId) {
        QueryWrapper<TaskReportPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1)
             .orderByDesc("report_version")
             .orderByDesc("id")
             .last("LIMIT 1");
        TaskReportPo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TaskReport> listByTaskId(Long taskId) {
        QueryWrapper<TaskReportPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1)
             .orderByDesc("report_version");
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
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
        return new TaskReport()
                .setId(po.getId())
                .setTaskId(po.getTaskId())
                .setReportVersion(po.getReportVersion())
                .setCompleteRate(po.getCompleteRate())
                .setSuccessRate(po.getSuccessRate())
                .setFailCaseDist(po.getFailCaseDist())
                .setGenTime(po.getGenTime() != null ? po.getGenTime().toInstant() : null);
    }

    private TaskReportPo toPo(TaskReport domain) {
        return TaskReportPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .reportVersion(domain.getReportVersion() != null ? domain.getReportVersion() : 1)
                .completeRate(domain.getCompleteRate())
                .successRate(domain.getSuccessRate())
                .failCaseDist(domain.getFailCaseDist())
                .genTime(domain.getGenTime() != null ? Date.from(domain.getGenTime()) : new Date())
                .build();
    }
}
