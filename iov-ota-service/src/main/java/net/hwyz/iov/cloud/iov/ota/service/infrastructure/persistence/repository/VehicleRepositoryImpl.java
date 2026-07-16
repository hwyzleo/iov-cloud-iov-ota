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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车辆仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleRepositoryImpl extends AbstractRepository<String, VehicleDo> implements VehicleRepository {

    private final CacheService cacheService;
    private final VehStatusMapper vehStatusDao;
    private final VehicleFactory vehicleFactory;
    private final VmdVehicleService exVehicleService;

    @Override
    public Optional<VehicleDo> getById(String vin) {
        return Optional.ofNullable(cacheService.getVehicle(vin).orElseGet(() -> {
            VehStatusPo vehicleStatus = vehStatusDao.selectByVin(vin);
            VehicleDo tmpVehicle;
            if (vehicleStatus == null) {
                VehicleExResponse vehicle = exVehicleService.getByVin(vin);
                if (vehicle == null) {
                    return null;
                }
                tmpVehicle = vehicleFactory.buildVehicle(vehicle);
            } else {
                tmpVehicle = vehicleFactory.buildVehicle(vehicleStatus);
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
        // 使用MyBatis-Plus QueryWrapper构建条件查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehStatusPo> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
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
        java.util.List<VehStatusPo> poList = vehStatusDao.selectList(queryWrapper);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehStatusPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicle(po);
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
            default -> null;
        };
    }

    @Override
    public java.util.List<VehicleDo> findByVins(java.util.List<String> vins) {
        if (vins == null || vins.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 使用MyBatis-Plus QueryWrapper批量查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehStatusPo> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.in("vin", vins);
        
        java.util.List<VehStatusPo> poList = vehStatusDao.selectList(queryWrapper);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehStatusPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicle(po);
            result.add(vehicle);
        }
        
        log.info("批量查询车辆完成，请求VIN数[{}]，结果数: {}", vins.size(), result.size());
        return result;
    }

    @Override
    public java.util.List<VehicleDo> findAll() {
        // 查询所有车辆
        java.util.List<VehStatusPo> poList = vehStatusDao.selectList(null);
        
        // 转换为领域对象
        java.util.List<VehicleDo> result = new java.util.ArrayList<>();
        for (VehStatusPo po : poList) {
            VehicleDo vehicle = vehicleFactory.buildVehicle(po);
            result.add(vehicle);
        }
        
        log.info("查询所有车辆完成，结果数: {}", result.size());
        return result;
    }

}
