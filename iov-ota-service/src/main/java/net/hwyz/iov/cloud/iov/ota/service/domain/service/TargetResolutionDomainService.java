package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;

/**
 * 目标解析领域服务
 * 解析任务目标定义，返回车辆集合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetResolutionDomainService {
    
    private final VehicleRepository vehicleRepository;
    
    /**
     * 解析目标定义，返回车辆集合
     * @param target 目标定义（JSON字符串）
     * @return 车辆集合
     */
    public Set<Vin> resolveTarget(String target) {
        if (target == null || target.isEmpty()) {
            return new HashSet<>();
        }
        
        // 尝试解析JSON格式
        try {
            JSONObject json = new JSONObject(target);
            String mode = json.getStr("mode");
            if (mode != null) {
                return switch (mode) {
                    case "LIST" -> resolveListMode(json);
                    case "IMPORT" -> resolveImportMode(json);
                    case "CONDITION" -> resolveConditionMode(json);
                    default -> {
                        log.warn("未知的目标模式: {}", mode);
                        yield new HashSet<>();
                    }
                };
            }
        } catch (Exception e) {
            log.debug("目标不是JSON格式，尝试旧格式解析: {}", e.getMessage());
        }
        
        // 兼容旧格式：逗号分隔的VIN列表
        return parseLegacyTarget(target);
    }
    
    /**
     * 解析LIST模式
     */
    private Set<Vin> resolveListMode(JSONObject json) {
        List<String> vins = json.getJSONArray("vins").toList(String.class);
        return vins.stream().map(Vin::of).collect(Collectors.toSet());
    }
    
    /**
     * 解析IMPORT模式
     * 支持两种格式：
     * 1. 直接包含vins数组：{"mode":"IMPORT","vins":["VIN001","VIN002"]}
     * 2. 包含fileId，需要从文件服务解析（暂未实现）
     */
    private Set<Vin> resolveImportMode(JSONObject json) {
        if (json.containsKey("vins")) {
            List<String> vins = json.getJSONArray("vins").toList(String.class);
            if (vins == null || vins.isEmpty()) {
                log.warn("IMPORT模式vins数组为空");
                return new HashSet<>();
            }
            
            // 验证VIN是否存在于系统中
            List<VehicleDo> vehicles = vehicleRepository.findByVins(vins);
            Set<Vin> existingVins = vehicles.stream()
                .map(vehicle -> Vin.of(vehicle.getId()))
                .collect(Collectors.toSet());
            
            // 检查是否有不存在的VIN
            Set<String> requestedVins = new HashSet<>(vins);
            Set<String> existingVinStrings = vehicles.stream()
                .map(VehicleDo::getId)
                .collect(Collectors.toSet());
            
            requestedVins.removeAll(existingVinStrings);
            if (!requestedVins.isEmpty()) {
                log.warn("IMPORT模式中以下VIN不存在于系统: {}", requestedVins);
            }
            
            log.info("IMPORT模式解析完成，请求VIN数[{}]，存在车辆数[{}]", vins.size(), existingVins.size());
            return existingVins;
        } else if (json.containsKey("fileId")) {
            // TODO: 从文件服务解析VIN列表
            // 预期流程：
            // 1. 根据fileId从文件服务获取文件内容
            // 2. 解析文件中的VIN列表（支持CSV/Excel格式）
            // 3. 验证VIN是否存在于系统中
            // 4. 返回车辆集合
            String fileId = json.getStr("fileId");
            log.warn("IMPORT模式文件解析暂未实现，fileId: {}", fileId);
            
            // 临时方案：从数据库查询所有车辆（仅用于测试）
            // 正式实现应对接文件服务
            throw new UnsupportedOperationException("IMPORT模式文件解析暂未实现，请使用vins数组方式");
        } else {
            log.warn("IMPORT模式缺少vins或fileId字段");
            return new HashSet<>();
        }
    }
    
    /**
     * 解析CONDITION模式
     * 条件格式：{"mode":"CONDITION","logic":"AND","conditions":[{"field":"baselineCode","operator":"=","value":"BL001"}]}
     * logic: AND=所有条件都满足，OR=任一条件满足
     */
    private Set<Vin> resolveConditionMode(JSONObject json) {
        JSONArray conditions = json.getJSONArray("conditions");
        if (conditions == null || conditions.isEmpty()) {
            log.warn("CONDITION模式缺少conditions数组");
            return new HashSet<>();
        }
        
        String logic = json.getStr("logic", "AND");
        if (!"AND".equals(logic) && !"OR".equals(logic)) {
            log.warn("CONDITION模式logic字段值无效[{}]，使用默认AND", logic);
            logic = "AND";
        }
        
        // 查询所有车辆
        List<VehicleDo> allVehicles = vehicleRepository.findAll();
        if (allVehicles == null || allVehicles.isEmpty()) {
            log.warn("CONDITION模式查询无车辆数据");
            return new HashSet<>();
        }
        
        // 根据逻辑关系过滤车辆
        Set<Vin> result = new HashSet<>();
        for (VehicleDo vehicle : allVehicles) {
            boolean matches = "AND".equals(logic) 
                ? matchesAllConditions(vehicle, conditions)
                : matchesAnyCondition(vehicle, conditions);
            if (matches) {
                result.add(Vin.of(vehicle.getId()));
            }
        }
        
        log.info("CONDITION模式查询完成，逻辑[{}]，条件数[{}]，匹配车辆数: {}", 
                logic, conditions.size(), result.size());
        
        return result;
    }
    
    /**
     * 检查车辆是否满足所有条件（AND逻辑）
     */
    private boolean matchesAllConditions(VehicleDo vehicle, JSONArray conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            if (!matchesCondition(vehicle, condition)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查车辆是否满足任一条件（OR逻辑）
     */
    private boolean matchesAnyCondition(VehicleDo vehicle, JSONArray conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            if (matchesCondition(vehicle, condition)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查车辆是否满足所有条件
     */
    private boolean matchesConditions(VehicleDo vehicle, JSONArray conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            if (!matchesCondition(vehicle, condition)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查车辆是否满足单个条件
     */
    private boolean matchesCondition(VehicleDo vehicle, JSONObject condition) {
        String field = condition.getStr("field");
        String operator = condition.getStr("operator");
        String value = condition.getStr("value");
        
        if (field == null || operator == null || value == null) {
            log.warn("条件格式错误: {}", condition);
            return false;
        }
        
        // 获取车辆字段值
        String actualValue = getFieldValue(vehicle, field);
        if (actualValue == null) {
            return false;
        }
        
        // 执行比较
        return switch (operator) {
            case "=" -> actualValue.equals(value);
            case "!=" -> !actualValue.equals(value);
            case ">" -> actualValue.compareTo(value) > 0;
            case ">=" -> actualValue.compareTo(value) >= 0;
            case "<" -> actualValue.compareTo(value) < 0;
            case "<=" -> actualValue.compareTo(value) <= 0;
            case "contains" -> actualValue.contains(value);
            case "startsWith" -> actualValue.startsWith(value);
            case "endsWith" -> actualValue.endsWith(value);
            default -> {
                log.warn("不支持的操作符: {}", operator);
                yield false;
            }
        };
    }
    
    /**
     * 获取车辆字段值
     */
    private String getFieldValue(VehicleDo vehicle, String field) {
        return switch (field) {
            case "vin" -> vehicle.getId();
            case "baselineCode" -> vehicle.getBaselineCode();
            case "isBaselineAlignment" -> String.valueOf(vehicle.getIsBaselineAlignment());
            // 可以扩展更多字段
            default -> {
                log.warn("不支持的字段: {}", field);
                yield null;
            }
        };
    }
    
    /**
     * 解析旧格式目标（逗号分隔的VIN列表）
     */
    private Set<Vin> parseLegacyTarget(String target) {
        return java.util.Arrays.stream(target.split(","))
                .map(Vin::of)
                .collect(Collectors.toSet());
    }
}