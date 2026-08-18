package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskMetric;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskMetricRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskMetricMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskMetricPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TaskMetric Repository实现（CR-015：Task 级统计，无 batchNo）
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
    public Optional<TaskMetric> findLatestByTaskId(Long taskId) {
        QueryWrapper<TaskMetricPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1)
             .orderByDesc("stat_time")
             .orderByDesc("id")
             .last("LIMIT 1");
        TaskMetricPo po = mapper.selectOne(query);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TaskMetric> listByTaskId(Long taskId, int limit) {
        QueryWrapper<TaskMetricPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1)
             .orderByDesc("stat_time")
             .orderByDesc("id")
             .last("LIMIT " + limit);
        return mapper.selectList(query).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
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
        return new TaskMetric()
                .setId(po.getId())
                .setTaskId(po.getTaskId())
                .setSuccessCnt(po.getSuccessCnt())
                .setFailCnt(po.getFailCnt())
                .setTimeoutCnt(po.getTimeoutCnt())
                .setFailRate(po.getFailRate())
                .setGateThreshold(po.getGateThreshold())
                .setGateState(po.getGateState())
                .setStatTime(po.getStatTime() != null ? po.getStatTime().toInstant() : null);
    }

    private TaskMetricPo toPo(TaskMetric domain) {
        return TaskMetricPo.builder()
                .id(domain.getId())
                .taskId(domain.getTaskId())
                .successCnt(domain.getSuccessCnt())
                .failCnt(domain.getFailCnt())
                .timeoutCnt(domain.getTimeoutCnt())
                .failRate(domain.getFailRate())
                .gateThreshold(domain.getGateThreshold())
                .gateState(domain.getGateState())
                .statTime(domain.getStatTime() != null ? Date.from(domain.getStatTime()) : new Date())
                .createTime(new Date())
                .modifyTime(new Date())
                .rowValid(true)
                .build();
    }
}
