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
public class TaskRestrictionCmd {
    
    private Long id;
    
    @NotBlank(message = "限制类型不能为空")
    private String type;
    
    @NotBlank(message = "限制表达式不能为空")
    private String expression;
}