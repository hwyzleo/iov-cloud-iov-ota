package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleProjectionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 车辆主档本地只读投影仓储实现
 * <p>
 * CR-011: 消费 MDM/VMD VehicleProduceEvent
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleProjectionRepositoryImpl implements VehicleProjectionRepository {

    private final VehicleProjectionMapper vehicleProjectionMapper;

    @Override
    public Optional<VehicleProjectionPo> findByVin(String vin) {
        return Optional.ofNullable(vehicleProjectionMapper.selectByVin(vin));
    }

    @Override
    public Optional<VehicleProjectionPo> findByVinForUpdate(String vin) {
        return Optional.ofNullable(vehicleProjectionMapper.selectByVinForUpdate(vin));
    }

    @Override
    public void insert(VehicleProjectionPo po) {
        vehicleProjectionMapper.insertPo(po);
        log.debug("插入车辆投影: vin={}", po.getVin());
    }

    @Override
    public void update(VehicleProjectionPo po) {
        vehicleProjectionMapper.updatePo(po);
        log.debug("更新车辆投影: vin={}", po.getVin());
    }

    @Override
    public boolean updateIfNewerVersion(VehicleProjectionPo po) {
        int affected = vehicleProjectionMapper.updateIfNewerVersion(po);
        if (affected > 0) {
            log.debug("按版本更新车辆投影成功: vin={}, sourceVersion={}", po.getVin(), po.getSourceVersion());
            return true;
        } else {
            log.debug("跳过旧版本车辆投影: vin={}, sourceVersion={}", po.getVin(), po.getSourceVersion());
            return false;
        }
    }

    @Override
    public List<VehicleProjectionPo> findAll() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.orderByDesc("id");
        return vehicleProjectionMapper.selectList(wrapper);
    }

    @Override
    public List<VehicleProjectionPo> findByConfigurationCode(String configurationCode) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("configuration_code", configurationCode);
        wrapper.orderByDesc("id");
        return vehicleProjectionMapper.selectList(wrapper);
    }

    @Override
    public List<VehicleProjectionPo> findByVins(List<String> vins) {
        if (vins == null || vins.isEmpty()) {
            return new ArrayList<>();
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehicleProjectionPo> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.in("vin", vins);
        return vehicleProjectionMapper.selectList(wrapper);
    }
}
