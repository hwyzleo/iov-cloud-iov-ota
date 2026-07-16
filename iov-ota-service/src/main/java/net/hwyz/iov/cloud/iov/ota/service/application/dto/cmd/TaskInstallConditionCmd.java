package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * 任务安装条件命令对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstallConditionCmd {

    private Long id;

    @NotBlank(message = "条件类型不能为空")
    private String conditionType;

    @NotBlank(message = "操作符不能为空")
    private String operator;

    @NotBlank(message = "阈值不能为空")
    private String threshold;

    @NotBlank(message = "严重级别不能为空")
    private String severity;

    private String description;
}
