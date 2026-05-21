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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.VehStatusPo;
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

}
