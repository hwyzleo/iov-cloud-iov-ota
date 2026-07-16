package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskInstallConditionMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskRestrictionMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskStrategyMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.InstallConditionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskMptAssembler {

    public TaskCreateCmd toCmd(TaskMpt vo) {
        if (vo == null) return null;
        return TaskCreateCmd.builder()
            .name(vo.getName())
            .type(vo.getType() != null ? String.valueOf(vo.getType()) : null)
            .activityId(vo.getActivityId())
            .target(vo.getTarget())
            .startTime(toInstant(vo.getStartTime()))
            .endTime(toInstant(vo.getEndTime()))
            .noticeType(vo.getNoticeType())
            .upgradeMode(vo.getUpgradeMode() != null ? String.valueOf(vo.getUpgradeMode()) : null)
            .upgradeModeArg(vo.getUpgradeModeArg())
            .restrictions(toRestrictionCmdList(vo.getRestrictions()))
            .installConditions(toInstallConditionCmdList(vo.getInstallConditions()))
            .strategies(toStrategyCmdList(vo.getStrategies()))
            .build();
    }

    public TaskSubmitCmd toSubmitCmd(TaskMpt vo) {
        if (vo == null) return null;
        return TaskSubmitCmd.builder()
            .taskId(vo.getId())
            .name(vo.getName())
            .type(vo.getType() != null ? String.valueOf(vo.getType()) : null)
            .startTime(toInstant(vo.getStartTime()))
            .endTime(toInstant(vo.getEndTime()))
            .noticeType(vo.getNoticeType())
            .upgradeMode(vo.getUpgradeMode() != null ? String.valueOf(vo.getUpgradeMode()) : null)
            .upgradeModeArg(vo.getUpgradeModeArg())
            .restrictions(toRestrictionCmdList(vo.getRestrictions()))
            .installConditions(toInstallConditionCmdList(vo.getInstallConditions()))
            .strategies(toStrategyCmdList(vo.getStrategies()))
            .build();
    }

    public TaskAuditCmd toAuditCmd(net.hwyz.iov.cloud.iov.ota.api.vo.TaskAuditMpt vo) {
        if (vo == null) return null;
        return TaskAuditCmd.builder()
            .taskId(vo.getId())
            .approvalLevel(vo.getApprovalLevel())
            .result(vo.getResult())
            .comment(vo.getComment())
            .build();
    }

    public TaskMpt toVo(TaskResult result) {
        if (result == null) return null;
        TaskMpt vo = new TaskMpt();
        vo.setId(result.getTaskId());
        vo.setName(result.getName());
        vo.setType(TaskType.valueOf(result.getType()).getValue());
        vo.setPhase(TaskPhase.valueOf(result.getPhase()).getValue());
        vo.setState(TaskState.valueOf(result.getState()).value);
        vo.setActivityId(result.getActivityId());
        vo.setTarget(result.getTarget());
        vo.setStartTime(toDate(result.getStartTime()));
        vo.setEndTime(toDate(result.getEndTime()));
        vo.setReleaseTime(toDate(result.getReleaseTime()));
        vo.setNoticeType(result.getNoticeType());
        vo.setUpgradeMode(result.getUpgradeMode() != null ? UpgradeMode.valueOf(result.getUpgradeMode()).getValue() : null);
        vo.setUpgradeModeArg(result.getUpgradeModeArg());
        return vo;
    }

    public List<TaskMpt> toVoList(List<TaskResult> results) {
        return results.stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    public TaskMpt fromPo(TaskPo taskPo, List<TaskRestrictionPo> restrictions, List<TaskStrategyPo> strategies,
                          List<TaskInstallCondition> installConditions, Map<String, InstallConditionType> conditionTypeMap) {
        if (taskPo == null) return null;

        TaskMpt taskMpt = new TaskMpt();
        taskMpt.setId(taskPo.getId());
        taskMpt.setName(taskPo.getName());
        taskMpt.setType(taskPo.getType());
        taskMpt.setPhase(taskPo.getPhase());
        taskMpt.setActivityId(taskPo.getActivityId());
        taskMpt.setTarget(taskPo.getTarget());
        taskMpt.setStartTime(taskPo.getStartTime());
        taskMpt.setEndTime(taskPo.getEndTime());
        taskMpt.setReleaseTime(taskPo.getReleaseTime());
        taskMpt.setNoticeType(taskPo.getNoticeType());
        taskMpt.setUpgradeMode(taskPo.getUpgradeMode());
        taskMpt.setUpgradeModeArg(taskPo.getUpgradeModeArg());
        taskMpt.setState(taskPo.getState());
        taskMpt.setCreateTime(taskPo.getCreateTime());

        if (restrictions != null) {
            taskMpt.setRestrictions(restrictions.stream()
                .map(po -> TaskRestrictionMpt.builder()
                    .id(po.getId())
                    .type(po.getRestrictionType())
                    .expression(po.getRestrictionExpression())
                    .build())
                .collect(Collectors.toList()));
        }

        if (installConditions != null) {
            taskMpt.setInstallConditions(installConditions.stream()
                .map(ic -> {
                    InstallConditionType typeInfo = conditionTypeMap != null ? conditionTypeMap.get(ic.getConditionType()) : null;
                    return TaskInstallConditionMpt.builder()
                        .id(ic.getId())
                        .conditionType(ic.getConditionType())
                        .conditionName(typeInfo != null ? typeInfo.getName() : null)
                        .operator(ic.getOperator())
                        .threshold(ic.getThreshold())
                        .unit(typeInfo != null ? typeInfo.getUnit() : null)
                        .severity(ic.getSeverity())
                        .description(ic.getDescription())
                        .build();
                })
                .collect(Collectors.toList()));
        }

        if (strategies != null) {
            taskMpt.setStrategies(strategies.stream()
                .map(po -> TaskStrategyMpt.builder()
                    .id(po.getId())
                    .type(po.getStrategyType())
                    .strategy(po.getStrategyExpression())
                    .build())
                .collect(Collectors.toList()));
        }

        return taskMpt;
    }

    public TaskPo toPo(TaskMpt taskMpt) {
        if (taskMpt == null) return null;

        TaskPo taskPo = new TaskPo();
        taskPo.setId(taskMpt.getId());
        taskPo.setName(taskMpt.getName());
        taskPo.setType(taskMpt.getType());
        taskPo.setPhase(taskMpt.getPhase());
        taskPo.setActivityId(taskMpt.getActivityId());
        taskPo.setTarget(taskMpt.getTarget());
        taskPo.setStartTime(taskMpt.getStartTime());
        taskPo.setEndTime(taskMpt.getEndTime());
        taskPo.setReleaseTime(taskMpt.getReleaseTime());
        taskPo.setNoticeType(taskMpt.getNoticeType());
        taskPo.setUpgradeMode(taskMpt.getUpgradeMode());
        taskPo.setUpgradeModeArg(taskMpt.getUpgradeModeArg());
        taskPo.setState(taskMpt.getState());

        return taskPo;
    }

    public List<TaskRestrictionPo> toRestrictionPoList(Long taskId, List<TaskRestrictionMpt> restrictions) {
        if (restrictions == null) return List.of();

        return restrictions.stream()
            .map(r -> TaskRestrictionPo.builder()
                .id(r.getId())
                .taskId(taskId)
                .restrictionType(r.getType())
                .restrictionExpression(r.getExpression())
                .build())
            .collect(Collectors.toList());
    }

    public List<TaskInstallCondition> toInstallConditionList(Long taskId, List<TaskInstallConditionMpt> conditions) {
        if (conditions == null) return List.of();

        return conditions.stream()
            .map(c -> TaskInstallCondition.builder()
                .id(c.getId())
                .taskId(taskId)
                .conditionType(c.getConditionType())
                .operator(c.getOperator())
                .threshold(c.getThreshold())
                .severity(c.getSeverity())
                .description(c.getDescription())
                .build())
            .collect(Collectors.toList());
    }

    public List<TaskStrategyPo> toStrategyPoList(Long taskId, List<TaskStrategyMpt> strategies) {
        if (strategies == null) return List.of();

        return strategies.stream()
            .map(s -> TaskStrategyPo.builder()
                .id(s.getId())
                .taskId(taskId)
                .strategyType(s.getType())
                .strategyExpression(s.getStrategy())
                .build())
            .collect(Collectors.toList());
    }

    public List<TaskMpt> fromPoList(List<TaskPo> taskPoList) {
        return taskPoList.stream()
            .map(po -> fromPo(po, null, null, null, null))
            .collect(Collectors.toList());
    }

    private List<TaskRestrictionCmd> toRestrictionCmdList(List<TaskRestrictionMpt> restrictions) {
        if (restrictions == null) return null;

        return restrictions.stream()
            .map(r -> TaskRestrictionCmd.builder()
                .id(r.getId())
                .type(r.getType())
                .expression(r.getExpression())
                .build())
            .collect(Collectors.toList());
    }

    private List<TaskInstallConditionCmd> toInstallConditionCmdList(List<TaskInstallConditionMpt> conditions) {
        if (conditions == null) return null;

        return conditions.stream()
            .map(c -> TaskInstallConditionCmd.builder()
                .id(c.getId())
                .conditionType(c.getConditionType())
                .operator(c.getOperator())
                .threshold(c.getThreshold())
                .severity(c.getSeverity())
                .description(c.getDescription())
                .build())
            .collect(Collectors.toList());
    }

    private List<TaskStrategyCmd> toStrategyCmdList(List<TaskStrategyMpt> strategies) {
        if (strategies == null) return null;

        return strategies.stream()
            .map(s -> TaskStrategyCmd.builder()
                .id(s.getId())
                .type(s.getType())
                .strategy(s.getStrategy())
                .build())
            .collect(Collectors.toList());
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
