package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TaskResult {
    
    private Long taskId;
    
    private String name;
    
    private String type;
    
    private String state;
    
    private String phase;
    
    private Long activityId;
    
    private Integer sequenceNo;
    
    private Long previousTaskId;
    
    /** 前序任务名称（IOV-OTA-DSN-CR-017 §6.2 只读展示） */
    private String previousTaskName;
    
    /** 前序任务阶段（IOV-OTA-DSN-CR-017 §6.2 只读展示） */
    private String previousPhase;
    
    /** 前序任务正式报告状态：REPORTED / NONE（IOV-OTA-DSN-CR-017 §6.2 只读展示） */
    private String previousReportState;
    
    /** 当前任务放行门禁状态：PASS / FAIL / PENDING（IOV-OTA-DSN-CR-017 §6.2 只读展示） */
    private String releaseGateState;
    
    private String target;
    
    private Instant startTime;
    
    private Instant endTime;
    
    private Instant releaseTime;
    
    private String description;
    
    private String noticeType;
    
    private String upgradeMode;
    
    private String upgradeModeArg;
    
    private List<TaskRestrictionResult> restrictions;
    
    private List<TaskStrategyResult> strategies;
}