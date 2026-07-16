package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;

/**
 * 管理后台升级任务安装条件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstallConditionMpt {

    /**
     * 主键
     */
    private Long id;

    /**
     * 条件类型编码（InstallConditionType.code）
     */
    private String conditionType;

    /**
     * 条件名称（只读，来自 InstallConditionType.name）
     */
    private String conditionName;

    /**
     * 操作符：EQ/NE/GT/GE/LT/LE/IN/NOT_IN
     */
    private String operator;

    /**
     * 阈值
     */
    private String threshold;

    /**
     * 单位（只读，来自 InstallConditionType.unit）
     */
    private String unit;

    /**
     * 严重级别：BLOCK/WARN
     */
    private String severity;

    /**
     * 备注
     */
    private String description;
}
