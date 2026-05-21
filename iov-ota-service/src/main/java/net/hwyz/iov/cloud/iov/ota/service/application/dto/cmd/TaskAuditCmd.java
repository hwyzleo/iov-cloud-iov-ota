package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAuditCmd {
    
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    
    private String reason;
}