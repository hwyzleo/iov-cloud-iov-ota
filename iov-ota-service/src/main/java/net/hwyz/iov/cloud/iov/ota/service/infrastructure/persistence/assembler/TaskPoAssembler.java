package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestriction;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStrategy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskPoAssembler {

    public Task fromPo(TaskPo taskPo,
                       List<TaskRestrictionPo> restrictionPoList,
                       List<TaskStrategyPo> strategyPoList) {
        TaskId taskId = TaskId.of(taskPo.getId());
        
        Task task = Task.fromPo(taskId);
        task.setName(taskPo.getName());
        task.setType(TaskType.valOf(taskPo.getType()));
        task.setPhase(TaskPhase.valOf(taskPo.getPhase()));
        task.setActivityId(ActivityId.of(taskPo.getActivityId()));
        task.setTarget(taskPo.getTarget());
        task.setStartTime(toInstant(taskPo.getStartTime()));
        task.setEndTime(toInstant(taskPo.getEndTime()));
        task.setReleaseTime(toInstant(taskPo.getReleaseTime()));
        task.setActualReleaseTime(toInstant(taskPo.getActualReleaseTime()));
        task.setLastScheduleError(taskPo.getLastScheduleError());
        task.setNoticeType(taskPo.getNoticeType());
        task.setUpgradeMode(UpgradeMode.valOf(taskPo.getUpgradeMode()));
        task.setUpgradeModeArg(net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.UpgradeModeArg.fromJson(taskPo.getUpgradeModeArg()));
        task.setState(TaskState.valOf(taskPo.getState()));
        task.setStateBeforePause(TaskState.valOf(taskPo.getStateBeforePause()));
        task.setDescription(taskPo.getDescription());

        List<TaskRestriction> restrictions = restrictionPoList.stream()
            .map(this::toRestriction)
            .collect(Collectors.toList());
        
        List<TaskStrategy> strategies = strategyPoList.stream()
            .map(this::toStrategy)
            .collect(Collectors.toList());
        
        task.loadRestrictionsAndStrategies(restrictions, strategies);
        
        return task;
    }

    public TaskPo toTaskPo(Task task) {
        TaskPo po = new TaskPo();
        po.setId(task.getId().getValue());
        po.setName(task.getName());
        po.setType(task.getType().getValue());
        po.setPhase(task.getPhase().getValue());
        po.setActivityId(task.getActivityId().getValue());
        po.setTarget(task.getTarget());
        po.setStartTime(toDate(task.getStartTime()));
        po.setEndTime(toDate(task.getEndTime()));
        po.setReleaseTime(toDate(task.getReleaseTime()));
        po.setActualReleaseTime(toDate(task.getActualReleaseTime()));
        po.setLastScheduleError(task.getLastScheduleError());
        po.setNoticeType(task.getNoticeType());
        po.setUpgradeMode(task.getUpgradeMode().getValue());
        po.setUpgradeModeArg(task.getUpgradeModeArg() != null ? task.getUpgradeModeArg().toJson() : null);
        po.setState(task.getState().value);
        po.setStateBeforePause(task.getStateBeforePause() != null ? task.getStateBeforePause().value : null);
        po.setDescription(task.getDescription());
        return po;
    }

    public List<TaskRestrictionPo> toRestrictionPoList(Task task) {
        if (task.getRestrictions() == null) {
            return List.of();
        }
        return task.getRestrictions().values().stream()
            .map(r -> {
                TaskRestrictionPo po = new TaskRestrictionPo();
                po.setId(r.getId());
                po.setTaskId(task.getId().getValue());
                po.setRestrictionType(r.getType().name());
                po.setRestrictionExpression(r.getExpression());
                return po;
            })
            .collect(Collectors.toList());
    }

    public List<TaskStrategyPo> toStrategyPoList(Task task) {
        if (task.getStrategies() == null) {
            return List.of();
        }
        return task.getStrategies().stream()
            .map(s -> {
                TaskStrategyPo po = new TaskStrategyPo();
                po.setId(s.getId());
                po.setTaskId(task.getId().getValue());
                po.setStrategyType(s.getType().name());
                po.setStrategyExpression(s.getStrategy());
                return po;
            })
            .collect(Collectors.toList());
    }

    private TaskRestriction toRestriction(TaskRestrictionPo po) {
        return TaskRestriction.builder()
            .id(po.getId())
            .taskId(TaskId.of(po.getTaskId()))
            .type(TaskRestrictionType.valueOf(po.getRestrictionType()))
            .expression(po.getRestrictionExpression())
            .build();
    }

    private TaskStrategy toStrategy(TaskStrategyPo po) {
        return TaskStrategy.builder()
            .id(po.getId())
            .taskId(TaskId.of(po.getTaskId()))
            .type(TaskStrategyType.valueOf(po.getStrategyType()))
            .strategy(po.getStrategyExpression())
            .build();
    }

    private Instant toInstant(Date date) {
        if (date == null) return null;
        return date.toInstant();
    }

    private Date toDate(Instant instant) {
        if (instant == null) return null;
        return Date.from(instant);
    }
}