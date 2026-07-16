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