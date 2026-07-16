package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 任务安装条件结果对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstallConditionResult {

    private Long id;
    private String conditionType;
    private String conditionName;
    private String operator;
    private String threshold;
    private String unit;
    private String severity;
    private String description;
}
