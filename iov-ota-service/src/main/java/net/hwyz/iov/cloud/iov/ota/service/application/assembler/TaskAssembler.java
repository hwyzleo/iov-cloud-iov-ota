package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition;
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
            .noticeType(task.getNoticeType())
            .upgradeMode(task.getUpgradeMode() != null ? task.getUpgradeMode().name() : null)
            .upgradeModeArg(task.getUpgradeModeArg() != null ? task.getUpgradeModeArg().toJson() : null)
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
        if (cmd.getType() != null && !cmd.getType().isEmpty()) {
            task.setType(TaskType.valOf(Integer.parseInt(cmd.getType())));
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
        if (cmd.getUpgradeMode() != null && !cmd.getUpgradeMode().isEmpty()) {
            task.setUpgradeMode(net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode.valOf(Integer.parseInt(cmd.getUpgradeMode())));
        }
        if (cmd.getUpgradeModeArg() != null && !cmd.getUpgradeModeArg().isEmpty()) {
            task.setUpgradeModeArg(net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.UpgradeModeArg.fromJson(cmd.getUpgradeModeArg()));
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

    public List<TaskInstallCondition> toInstallConditions(List<TaskInstallConditionCmd> cmdList) {
        if (cmdList == null) {
            return List.of();
        }
        return cmdList.stream()
            .map(cmd -> TaskInstallCondition.builder()
                .id(cmd.getId())
                .conditionType(cmd.getConditionType())
                .operator(cmd.getOperator())
                .threshold(cmd.getThreshold())
                .severity(cmd.getSeverity())
                .description(cmd.getDescription())
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