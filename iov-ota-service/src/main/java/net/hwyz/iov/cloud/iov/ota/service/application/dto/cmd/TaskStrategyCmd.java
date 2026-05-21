package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStrategyCmd {
    
    private Long id;
    
    @NotBlank(message = "策略类型不能为空")
    private String type;
    
    @NotBlank(message = "策略内容不能为空")
    private String strategy;
}