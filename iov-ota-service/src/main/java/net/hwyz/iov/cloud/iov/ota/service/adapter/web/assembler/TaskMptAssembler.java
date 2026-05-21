package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
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
            .type(String.valueOf(vo.getType()))
            .activityId(vo.getActivityId())
            .target(vo.getTarget())
            .startTime(toInstant(vo.getStartTime()))
            .endTime(toInstant(vo.getEndTime()))
            .noticeType(vo.getNoticeType())
            .upgradeMode(String.valueOf(vo.getUpgradeMode()))
            .upgradeModeArg(vo.getUpgradeModeArg())
            .build();
    }

    public TaskSubmitCmd toSubmitCmd(TaskMpt vo) {
        if (vo == null) return null;
        return TaskSubmitCmd.builder()
            .taskId(vo.getId())
            .name(vo.getName())
            .type(String.valueOf(vo.getType()))
            .startTime(toInstant(vo.getStartTime()))
            .endTime(toInstant(vo.getEndTime()))
            .noticeType(vo.getNoticeType())
            .upgradeMode(String.valueOf(vo.getUpgradeMode()))
            .upgradeModeArg(vo.getUpgradeModeArg())
            .build();
    }

    public TaskAuditCmd toAuditCmd(net.hwyz.iov.cloud.iov.ota.api.vo.TaskAuditMpt vo) {
        if (vo == null) return null;
        return TaskAuditCmd.builder()
            .taskId(vo.getId())
            .approved(vo.getAudit())
            .reason(vo.getReason())
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
        return vo;
    }

    public List<TaskMpt> toVoList(List<TaskResult> results) {
        return results.stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    public TaskMpt fromPo(TaskPo taskPo, List<TaskRestrictionPo> restrictions, List<TaskStrategyPo> strategies) {
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
            Map<String, String> restrictionMap = restrictions.stream()
                .collect(Collectors.toMap(TaskRestrictionPo::getRestrictionType, TaskRestrictionPo::getRestrictionExpression));
            
            if (restrictionMap.containsKey(TaskRestrictionType.ADAPTATION_SUBJECT.name())) {
                taskMpt.setAdaptiveSubject(Integer.parseInt(restrictionMap.get(TaskRestrictionType.ADAPTATION_SUBJECT.name())));
            }
            if (restrictionMap.containsKey(TaskRestrictionType.BASELINE_EXCLUDE.name())) {
                taskMpt.setExcludedBaseline(restrictionMap.get(TaskRestrictionType.BASELINE_EXCLUDE.name()));
            }
            if (restrictionMap.containsKey(TaskRestrictionType.BASELINE_UNIFICATION.name())) {
                taskMpt.setBaselineUnification(Boolean.parseBoolean(restrictionMap.get(TaskRestrictionType.BASELINE_UNIFICATION.name())));
            }
            if (restrictionMap.containsKey(TaskRestrictionType.COMPARISON_CRITERIA.name())) {
                taskMpt.setComparisonCriteria(Boolean.parseBoolean(restrictionMap.get(TaskRestrictionType.COMPARISON_CRITERIA.name())));
            }
        }
        
        if (strategies != null) {
            Map<String, String> strategyMap = strategies.stream()
                .collect(Collectors.toMap(TaskStrategyPo::getStrategyType, TaskStrategyPo::getStrategyExpression));
            
            if (strategyMap.containsKey(TaskStrategyType.ROLLBACK.name())) {
                taskMpt.setRollback(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.ROLLBACK.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.FLASH_COUNT.name())) {
                taskMpt.setFlashCount(Integer.parseInt(strategyMap.get(TaskStrategyType.FLASH_COUNT.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.IMPACT_VEHICLE_OPERATION.name())) {
                taskMpt.setImpactVehicleOperation(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.IMPACT_VEHICLE_OPERATION.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.KEEP_IN_PARK.name())) {
                taskMpt.setKeepInPark(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.KEEP_IN_PARK.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.NOT_CHARGING.name())) {
                taskMpt.setNotCharging(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.NOT_CHARGING.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.NO_EXTERNAL_POWER.name())) {
                taskMpt.setNoExternalPower(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.NO_EXTERNAL_POWER.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.ALL_CLOSED.name())) {
                taskMpt.setAllClosed(Boolean.parseBoolean(strategyMap.get(TaskStrategyType.ALL_CLOSED.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.HV_SOC.name())) {
                taskMpt.setHvSoc(Integer.parseInt(strategyMap.get(TaskStrategyType.HV_SOC.name())));
            }
            if (strategyMap.containsKey(TaskStrategyType.LV_SOC.name())) {
                taskMpt.setLvSoc(Integer.parseInt(strategyMap.get(TaskStrategyType.LV_SOC.name())));
            }
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

    public List<TaskRestrictionPo> toRestrictionPoList(TaskMpt taskMpt) {
        if (taskMpt == null) return List.of();
        
        List<TaskRestrictionPo> list = new java.util.ArrayList<>();
        
        if (taskMpt.getAdaptiveSubject() != null) {
            list.add(TaskRestrictionPo.builder()
                .taskId(taskMpt.getId())
                .restrictionType(TaskRestrictionType.ADAPTATION_SUBJECT.name())
                .restrictionExpression(String.valueOf(taskMpt.getAdaptiveSubject()))
                .build());
        }
        if (taskMpt.getExcludedBaseline() != null && !taskMpt.getExcludedBaseline().isBlank()) {
            list.add(TaskRestrictionPo.builder()
                .taskId(taskMpt.getId())
                .restrictionType(TaskRestrictionType.BASELINE_EXCLUDE.name())
                .restrictionExpression(taskMpt.getExcludedBaseline())
                .build());
        }
        if (taskMpt.getBaselineUnification() != null) {
            list.add(TaskRestrictionPo.builder()
                .taskId(taskMpt.getId())
                .restrictionType(TaskRestrictionType.BASELINE_UNIFICATION.name())
                .restrictionExpression(String.valueOf(taskMpt.getBaselineUnification()))
                .build());
        }
        if (taskMpt.getComparisonCriteria() != null) {
            list.add(TaskRestrictionPo.builder()
                .taskId(taskMpt.getId())
                .restrictionType(TaskRestrictionType.COMPARISON_CRITERIA.name())
                .restrictionExpression(String.valueOf(taskMpt.getComparisonCriteria()))
                .build());
        }
        
        return list;
    }

    public List<TaskStrategyPo> toStrategyPoList(TaskMpt taskMpt) {
        if (taskMpt == null) return List.of();
        
        List<TaskStrategyPo> list = new java.util.ArrayList<>();
        
        if (taskMpt.getRollback() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.ROLLBACK.name())
                .strategyExpression(String.valueOf(taskMpt.getRollback()))
                .build());
        }
        if (taskMpt.getFlashCount() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.FLASH_COUNT.name())
                .strategyExpression(String.valueOf(taskMpt.getFlashCount()))
                .build());
        }
        if (taskMpt.getImpactVehicleOperation() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.IMPACT_VEHICLE_OPERATION.name())
                .strategyExpression(String.valueOf(taskMpt.getImpactVehicleOperation()))
                .build());
        }
        if (taskMpt.getKeepInPark() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.KEEP_IN_PARK.name())
                .strategyExpression(String.valueOf(taskMpt.getKeepInPark()))
                .build());
        }
        if (taskMpt.getNotCharging() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.NOT_CHARGING.name())
                .strategyExpression(String.valueOf(taskMpt.getNotCharging()))
                .build());
        }
        if (taskMpt.getNoExternalPower() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.NO_EXTERNAL_POWER.name())
                .strategyExpression(String.valueOf(taskMpt.getNoExternalPower()))
                .build());
        }
        if (taskMpt.getAllClosed() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.ALL_CLOSED.name())
                .strategyExpression(String.valueOf(taskMpt.getAllClosed()))
                .build());
        }
        if (taskMpt.getHvSoc() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.HV_SOC.name())
                .strategyExpression(String.valueOf(taskMpt.getHvSoc()))
                .build());
        }
        if (taskMpt.getLvSoc() != null) {
            list.add(TaskStrategyPo.builder()
                .taskId(taskMpt.getId())
                .strategyType(TaskStrategyType.LV_SOC.name())
                .strategyExpression(String.valueOf(taskMpt.getLvSoc()))
                .build());
        }
        
        return list;
    }

    public List<TaskMpt> fromPoList(List<TaskPo> taskPoList) {
        return taskPoList.stream()
            .map(po -> fromPo(po, null, null))
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