package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskStateLogResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStateLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营审计查询服务（CR-015 §3.4）
 * <p>统一支持发生时间范围、阶段/结果、attemptNo 等筛选；
 * 按 decided_at DESC, id DESC 稳定分页。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationAuditQueryService {

    private final TaskStateLogMapper taskStateLogMapper;

    /**
     * 任务状态迁移审计查询
     *
     * @param taskId    任务ID
     * @param beginTime 发生时间下限（可空）
     * @param endTime   发生时间上限（可空）
     * @param action    操作类型（可空，如 SUBMIT/AUDIT/RELEASE）
     * @return 状态迁移日志（decided_at DESC, id DESC）
     */
    public List<TaskStateLogResult> listStateLogs(Long taskId, Date beginTime, Date endTime, String action) {
        QueryWrapper<TaskStateLogPo> query = new QueryWrapper<>();
        query.eq("task_id", taskId)
             .eq("row_valid", 1);
        if (beginTime != null) {
            query.ge("decided_at", beginTime);
        }
        if (endTime != null) {
            query.le("decided_at", endTime);
        }
        if (action != null && !action.isBlank()) {
            query.eq("action", action);
        }
        query.orderByDesc("decided_at")
             .orderByDesc("id");

        return taskStateLogMapper.selectList(query).stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }

    private TaskStateLogResult toResult(TaskStateLogPo po) {
        return TaskStateLogResult.builder()
                .taskId(po.getTaskId())
                .fromState(toStateName(po.getFromState()))
                .toState(toStateName(po.getToState()))
                .action(po.getAction())
                .operator(po.getOperator())
                .reason(po.getReason())
                .decidedAt(po.getDecidedAt() != null ? po.getDecidedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    private String toStateName(Integer value) {
        TaskState state = TaskState.valOf(value);
        return state != null ? state.name() : String.valueOf(value);
    }
}
