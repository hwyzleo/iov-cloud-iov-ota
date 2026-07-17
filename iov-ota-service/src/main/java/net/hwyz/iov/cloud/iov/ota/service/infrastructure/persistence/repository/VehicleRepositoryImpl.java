package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.factory.VehicleFactory;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehStatusPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车辆仓库接口实现类
 * <p>
 * CR-011: 优先从 tb_vehicle_projection 读取车辆信息
 * </p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleRepositoryImpl extends AbstractRepository<String, VehicleDo> implements VehicleRepository {

    private final CacheService cacheService;
    private final VehStatusMapper vehStatusDao;
    private final VehicleProjectionMapper vehicleProjectionDao;
    private final VehicleFactory vehicleFactory;
    private final VmdVehicleService exVehicleService;

    @Override
    public Optional<VehicleDo> getById(String vin) {
        return Optional.ofNullable(cacheService.getVehicle(vin).orElseGet(() -> {
            // CR-011: 优先从车辆投影表读取
            VehicleProjectionPo vehicleProjection = vehicleProjectionDao.selectByVin(vin);
            VehicleDo tmpVehicle;
            if (vehicleProjection != null) {
                tmpVehicle = vehicleFactory.buildVehicleFromProjection(vehicleProjection);
                log.debug("从车辆投影表获取车辆信息: vin={}", vin);
            } else {
                // 降级到车辆状态表
                VehStatusPo vehicleStatus = vehStatusDao.selectByVin(vin);
                if (vehicleStatus != null) {
                    tmpVehicle = vehicleFactory.buildVehicle(vehicleStatus);
                    log.debug("从车辆状态表获取车辆信息: vin={}", vin);
                } else {
                    // 最后尝试从VMD回源
                    VehicleExResponse vehicle = exVehicleService.getByVin(vin);
                    if (vehicle == null) {
                        return null;
                    }
                    tmpVehicle = vehicleFactory.buildVehicle(vehicle);
                    log.debug("从VMD回源获取车辆信息: vin={}", vin);
                }
            }
            cacheService.setVehicle(tmpVehicle);
            return tmpVehicle;
        }));
    }

    @Override
    public boolean save(VehicleDo vehicleDo) {
        switch (vehicleDo.getState()) {
            case NEW -> {
                VehStatusPo vehStatusPo = VehStatusPoAssembler.INSTANCE.fromDo(vehicleDo);
                vehStatusDao.insertPo(vehStatusPo);
            }
            case CHANGED -> {
                VehStatusPo vehStatusPo = VehStatusPoAssembler.INSTANCE.fromDo(vehicleDo);
                vehStatusDao.updatePo(vehStatusPo);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public java.util.List<VehicleDo> findByCondition(String field, String operator, String value) {
        // CR-011: 使用车辆投影表进行条件查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        // 映射字段名到数据库列名
        String columnName = mapFieldToColumn(field);
        if (columnName == null) {
            log.warn("不支持的查询字段: {}", field);
            return new java.util.ArrayList<>();
        }
        
        // 根据操作符构建查询条件
        switch (operator) {
            case "=" -> queryWrapper.eq(columnName, value);
            case "!=" -> queryWrapper.ne(columnName, value);
            case ">" -> queryWrapper.gt(columnName, value);
            case ">=" -> queryWrapper.ge(columnName, value);
            case "<" -> queryWrapper.lt(columnName, value);
            case "<=" -> queryWrapper.le(columnName, value);
            case "contains" -> queryWrapper.like(columnName, value);
            case "startsWith" -> queryWrapper.likeRight(columnName, value);
            case "endsWith" -> queryWrapper.likeLeft(columnName, value);
            default -> {
                log.warn("不支持的操作符: {}", operator);
                return new java.util.ArrayList<>();
            }
        }
        
        // 执行查询
        java.util.List<VehicleProjectionPo> poList = vehicleProjectionDao.selectList(queryWrapper);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehicleProjectionPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicleFromProjection(po);
            result.add(vehicle);
        }
        
        log.info("条件查询完成，字段[{}]，操作符[{}]，值[{}]，结果数: {}", field, operator, value, result.size());
        return result;
    }
    
    /**
     * 映射领域字段到数据库列名
     */
    private String mapFieldToColumn(String field) {
        return switch (field) {
            case "vin" -> "vin";
            case "baselineCode" -> "baseline_code";
            case "isBaselineAlignment" -> "is_baseline_alignment";
            case "configurationCode" -> "configuration_code";
            case "plantCode" -> "plant_code";
            case "brandCode" -> "brand_code";
            case "platformCode" -> "platform_code";
            case "modelCode" -> "model_code";
            case "variantCode" -> "variant_code";
            default -> null;
        };
    }

    @Override
    public java.util.List<VehicleDo> findByVins(java.util.List<String> vins) {
        if (vins == null || vins.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // CR-011: 使用车辆投影表批量查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.in("vin", vins);
        
        java.util.List<VehicleProjectionPo> poList = vehicleProjectionDao.selectList(queryWrapper);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehicleProjectionPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicleFromProjection(po);
            result.add(vehicle);
        }
        
        log.info("批量查询车辆完成，请求VIN数[{}]，结果数: {}", vins.size(), result.size());
        return result;
    }

    @Override
    public java.util.List<VehicleDo> findAll() {
        // CR-011: 查询所有车辆投影
        java.util.List<VehicleProjectionPo> poList = vehicleProjectionDao.selectList(null);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehicleProjectionPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicleFromProjection(po);
            result.add(vehicle);
        }
        
        log.info("查询所有车辆完成，结果数: {}", result.size());
        return result;
    }

}
