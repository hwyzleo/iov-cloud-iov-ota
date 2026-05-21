package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateCmd {
    
    @NotBlank(message = "任务名称不能为空")
    private String name;
    
    @NotNull(message = "任务类型不能为空")
    private String type;
    
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    
    @NotBlank(message = "升级对象不能为空")
    private String target;
    
    @NotNull(message = "开始时间不能为空")
    private Instant startTime;
    
    @NotNull(message = "结束时间不能为空")
    private Instant endTime;
    
    private String noticeType;
    
    @NotNull(message = "升级模式不能为空")
    private String upgradeMode;
    
    private String upgradeModeArg;
    
    private String description;
    
    private String createBy;
    
    private List<TaskRestrictionCmd> restrictions;
    
    private List<TaskStrategyCmd> strategies;
}