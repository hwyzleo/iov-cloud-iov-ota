package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务实例化安装条件领域实体
 * 对应表：tb_task_install_condition
 */
@Getter
@Setter
@Builder
public class TaskInstallCondition {
    
    private Long id;
    private Long taskId;
    private String conditionType;  // 条件类型编码
    private String operator;  // 操作符：EQ/NE/GT/GE/LT/LE/IN/NOT_IN
    private String threshold;  // 阈值
    private String severity;  // 严重级别：BLOCK/WARN
    private String description;  // 备注
    
    /**
     * 判断是否为阻断级别
     *
     * @return 是否为阻断级别
     */
    public boolean isBlockSeverity() {
        return "BLOCK".equals(severity);
    }
    
    /**
     * 判断是否为警告级别
     *
     * @return 是否为警告级别
     */
    public boolean isWarnSeverity() {
        return "WARN".equals(severity);
    }
    
    /**
     * 判断条件是否满足
     *
     * @param actualValue 实际值
     * @return 是否满足
     */
    public boolean isSatisfied(Object actualValue) {
        if (actualValue == null) {
            return false;
        }
        
        try {
            switch (operator) {
                case "EQ":
                    return String.valueOf(actualValue).equals(threshold);
                case "NE":
                    return !String.valueOf(actualValue).equals(threshold);
                case "GT":
                    return compareValues(actualValue, threshold) > 0;
                case "GE":
                    return compareValues(actualValue, threshold) >= 0;
                case "LT":
                    return compareValues(actualValue, threshold) < 0;
                case "LE":
                    return compareValues(actualValue, threshold) <= 0;
                case "IN":
                    return threshold.contains(String.valueOf(actualValue));
                case "NOT_IN":
                    return !threshold.contains(String.valueOf(actualValue));
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private int compareValues(Object actual, String expected) {
        String actualStr = String.valueOf(actual);
        try {
            double actualNum = Double.parseDouble(actualStr);
            double expectedNum = Double.parseDouble(expected);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            return actualStr.compareTo(expected);
        }
    }
}
