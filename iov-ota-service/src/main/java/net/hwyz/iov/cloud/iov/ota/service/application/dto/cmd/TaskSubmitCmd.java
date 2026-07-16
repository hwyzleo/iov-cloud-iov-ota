package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitCmd {
    
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    
    private String name;
    
    private String type;
    
    private Instant startTime;
    
    private Instant endTime;
    
    private String noticeType;
    
    private String upgradeMode;
    
    private String upgradeModeArg;
    
    private String modifyBy;

    private List<TaskRestrictionCmd> restrictions;

    private List<TaskInstallConditionCmd> installConditions;

    private List<TaskStrategyCmd> strategies;
}