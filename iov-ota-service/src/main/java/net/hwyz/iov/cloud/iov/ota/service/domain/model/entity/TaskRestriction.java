package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import cn.hutool.core.comparator.VersionComparator;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@Slf4j
public class TaskRestriction {
    
    private final Long id;
    private final TaskId taskId;
    private final TaskRestrictionType type;
    private final String expression;
    private final String restrictionLevel;  // ERROR/WARNING/INFO
    private final String errorMessage;  // 错误信息模板
    
    public boolean isSatisfiedBy(VehicleInfo vehicle) {
        switch (type) {
            case BASELINE_EXCLUDE:
                return checkBaselineExclude(vehicle);
            case BASELINE_UNIFICATION:
                return checkBaselineUnification(vehicle);
            case ADAPTATION_SUBJECT:
                return checkAdaptationSubject(vehicle);
            case COMPARISON_CRITERIA:
                return checkComparisonCriteria(vehicle);
            default:
                return true;
        }
    }
    
    private boolean checkBaselineExclude(VehicleInfo vehicle) {
        String[] excludedBaselines = expression.split(",");
        for (String baseline : excludedBaselines) {
            if (baseline.equals(vehicle.getBaselineCode())) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkBaselineUnification(VehicleInfo vehicle) {
        boolean forceAlignment = Boolean.parseBoolean(expression);
        if (!forceAlignment && !vehicle.getIsBaselineAlignment()) {
            return false;
        }
        return true;
    }
    
    private boolean checkAdaptationSubject(VehicleInfo vehicle) {
        // US-058/059：适配主体限制
        // expression格式：逗号分隔的适配主体编码列表（软件零件号）
        // 车辆的adaptationSubject（软件零件号）需要在允许列表中
        if (expression == null || expression.isEmpty()) {
            return true; // 没有限制条件
        }
        
        String adaptationSubject = vehicle.getAdaptationSubject();
        if (adaptationSubject == null || adaptationSubject.isEmpty()) {
            // 车辆没有适配主体信息，默认不满足
            log.debug("车辆[{}]没有适配主体信息，不满足适配主体限制", vehicle.getVin());
            return false;
        }
        
        String[] allowedSubjects = expression.split(",");
        for (String allowedSubject : allowedSubjects) {
            if (allowedSubject.trim().equals(adaptationSubject)) {
                return true;
            }
        }
        
        log.debug("车辆[{}]的适配主体[{}]不在允许列表[{}]中", vehicle.getVin(), adaptationSubject, expression);
        return false;
    }
    
    private boolean checkComparisonCriteria(VehicleInfo vehicle) {
        // US-058/059：比较标准限制
        // expression格式：比较表达式，如 "softwareVersion>=1.0.0" 或 "hardwareVersion<=2.0.0"
        // 支持操作符：>=, <=, >, <, =, !=
        if (expression == null || expression.isEmpty()) {
            return true; // 没有限制条件
        }
        
        // 解析表达式
        String[] operators = {">=", "<=", ">", "<", "=", "!="};
        String operator = null;
        String[] parts = null;
        
        for (String op : operators) {
            if (expression.contains(op)) {
                operator = op;
                parts = expression.split(op.replace("=", "\\="), 2);
                break;
            }
        }
        
        if (operator == null || parts == null || parts.length != 2) {
            log.warn("无效的比较表达式: {}", expression);
            return true; // 表达式格式错误，默认通过
        }
        
        String field = parts[0].trim();
        String expectedValue = parts[1].trim();
        
        // 获取车辆的实际值
        String actualValue = null;
        switch (field) {
            case "softwareVersion":
                actualValue = vehicle.getSoftwareVersion();
                break;
            case "hardwareVersion":
                actualValue = vehicle.getHardwareVersion();
                break;
            case "softwarePn":
                actualValue = vehicle.getSoftwarePn();
                break;
            case "hardwarePn":
                actualValue = vehicle.getHardwarePn();
                break;
            default:
                log.warn("不支持的比较字段: {}", field);
                return true; // 不支持的字段，默认通过
        }
        
        if (actualValue == null || actualValue.isEmpty()) {
            log.debug("车辆[{}]没有字段[{}]的值", vehicle.getVin(), field);
            return false; // 没有实际值，默认不满足
        }
        
        // 执行比较
        try {
            // 尝试作为版本号比较（使用Hutool的VersionComparator）
            int compareResult = cn.hutool.core.comparator.VersionComparator.INSTANCE.compare(actualValue, expectedValue);
            switch (operator) {
                case ">=": return compareResult >= 0;
                case "<=": return compareResult <= 0;
                case ">": return compareResult > 0;
                case "<": return compareResult < 0;
                case "=": return compareResult == 0;
                case "!=": return compareResult != 0;
                default: return true;
            }
        } catch (Exception e) {
            // 如果不是版本号，作为字符串比较
            int compareResult = actualValue.compareTo(expectedValue);
            switch (operator) {
                case ">=": return compareResult >= 0;
                case "<=": return compareResult <= 0;
                case ">": return compareResult > 0;
                case "<": return compareResult < 0;
                case "=": return compareResult == 0;
                case "!=": return compareResult != 0;
                default: return true;
            }
        }
    }
}