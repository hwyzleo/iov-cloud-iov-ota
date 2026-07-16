package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 安装条件类型结果对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallConditionTypeResult {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private String valueType;
    private String defaultValue;
    private String applicablePhase;
    private Boolean mandatory;
    private String description;
}
