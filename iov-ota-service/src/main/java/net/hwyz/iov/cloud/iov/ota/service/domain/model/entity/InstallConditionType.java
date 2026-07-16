package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 安装条件类型受控词表领域实体
 * 对应表：tb_install_condition_type
 */
@Getter
@Setter
@Builder
public class InstallConditionType {
    
    private Long id;
    private String code;  // 条件编码：KEEP_IN_PARK/NOT_CHARGING/NO_EXTERNAL_POWER/ALL_CLOSED/HV_SOC/LV_SOC/AMBIENT_TEMP/POWER_MODE/NETWORK_TYPE/SIGNAL_STRENGTH
    private String name;  // 条件名称
    private String unit;  // 单位
    private String valueType;  // 值类型：BOOLEAN/INTEGER/DECIMAL/STRING
    private String defaultValue;  // 默认值
    private String applicablePhase;  // 适用阶段（逗号分隔）：VALIDATION,CANARY,RELEASE
    private Boolean mandatory;  // 是否必选
    private String description;  // 备注
    
    /**
     * 判断是否适用于指定阶段
     *
     * @param phase 阶段
     * @return 是否适用
     */
    public boolean isApplicableForPhase(String phase) {
        if (applicablePhase == null || applicablePhase.isEmpty()) {
            return true;
        }
        String[] phases = applicablePhase.split(",");
        for (String p : phases) {
            if (p.trim().equals(phase)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为布尔类型
     *
     * @return 是否为布尔类型
     */
    public boolean isBooleanType() {
        return "BOOLEAN".equals(valueType);
    }
    
    /**
     * 判断是否为整数类型
     *
     * @return 是否为整数类型
     */
    public boolean isIntegerType() {
        return "INTEGER".equals(valueType);
    }
    
    /**
     * 判断是否为小数类型
     *
     * @return 是否为小数类型
     */
    public boolean isDecimalType() {
        return "DECIMAL".equals(valueType);
    }
    
    /**
     * 判断是否为字符串类型
     *
     * @return 是否为字符串类型
     */
    public boolean isStringType() {
        return "STRING".equals(valueType);
    }
}
