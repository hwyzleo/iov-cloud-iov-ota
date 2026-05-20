package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.factory.VehicleFactory;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.repository.VehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler.VehStatusPoAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.dao.VehStatusDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.VehStatusPo;
import net.hwyz.iov.cloud.tsp.vmd.api.contract.VehicleExService;
import net.hwyz.iov.cloud.tsp.vmd.api.feign.service.ExVehicleService;
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
    private final VehStatusDao vehStatusDao;
    private final VehicleFactory vehicleFactory;
    private final ExVehicleService exVehicleService;

    @Override
    public Optional<VehicleDo> getById(String vin) {
        return Optional.ofNullable(cacheService.getVehicle(vin).orElseGet(() -> {
            VehStatusPo vehicleStatus = vehStatusDao.selectByVin(vin);
            VehicleDo tmpVehicle;
            if (vehicleStatus == null) {
                VehicleExService vehicle = exVehicleService.getByVin(vin);
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
