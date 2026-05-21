package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestriction;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskAssembler {
    
    public TaskResult toResult(Task task) {
        TaskResult result = TaskResult.builder()
            .taskId(task.getId().getValue())
            .name(task.getName())
            .type(task.getType().name())
            .state(task.getState().name())
            .phase(task.getPhase().name())
            .activityId(task.getActivityId().getValue())
            .target(task.getTarget())
            .startTime(task.getStartTime())
            .endTime(task.getEndTime())
            .releaseTime(task.getReleaseTime())
            .description(task.getDescription())
            .build();
        
        if (task.getRestrictions() != null) {
            result.setRestrictions(task.getRestrictions().values().stream()
                .map(this::toRestrictionResult)
                .collect(Collectors.toList()));
        }
        
        if (task.getStrategies() != null) {
            result.setStrategies(task.getStrategies().stream()
                .map(this::toStrategyResult)
                .collect(Collectors.toList()));
        }
        
        return result;
    }
    
    public void updateFromCmd(Task task, TaskSubmitCmd cmd) {
        if (cmd.getName() != null) {
            task.setName(cmd.getName());
        }
        if (cmd.getType() != null) {
            task.setType(TaskType.valueOf(cmd.getType()));
        }
        if (cmd.getStartTime() != null) {
            task.setStartTime(cmd.getStartTime());
        }
        if (cmd.getEndTime() != null) {
            task.setEndTime(cmd.getEndTime());
        }
        if (cmd.getNoticeType() != null) {
            task.setNoticeType(cmd.getNoticeType());
        }
    }
    
    public List<TaskRestriction> toRestrictions(List<TaskRestrictionCmd> cmdList) {
        if (cmdList == null) {
            return List.of();
        }
        return cmdList.stream()
            .map(cmd -> TaskRestriction.builder()
                .id(cmd.getId())
                .type(TaskRestrictionType.valueOf(cmd.getType()))
                .expression(cmd.getExpression())
                .build())
            .collect(Collectors.toList());
    }
    
    public List<TaskStrategy> toStrategies(List<TaskStrategyCmd> cmdList) {
        if (cmdList == null) {
            return List.of();
        }
        return cmdList.stream()
            .map(cmd -> TaskStrategy.builder()
                .id(cmd.getId())
                .type(TaskStrategyType.valueOf(cmd.getType()))
                .strategy(cmd.getStrategy())
                .build())
            .collect(Collectors.toList());
    }
    
    private TaskRestrictionResult toRestrictionResult(TaskRestriction restriction) {
        return TaskRestrictionResult.builder()
            .id(restriction.getId())
            .type(restriction.getType().name())
            .expression(restriction.getExpression())
            .build();
    }
    
    private TaskStrategyResult toStrategyResult(TaskStrategy strategy) {
        return TaskStrategyResult.builder()
            .id(strategy.getId())
            .type(strategy.getType().name())
            .strategy(strategy.getStrategy())
            .build();
    }
}