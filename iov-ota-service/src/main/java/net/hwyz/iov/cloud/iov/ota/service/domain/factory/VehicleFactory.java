package net.hwyz.iov.cloud.iov.ota.service.domain.factory;

import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.VehStatusPo;
import net.hwyz.iov.cloud.tsp.vmd.api.contract.VehicleExService;
import org.springframework.stereotype.Component;

/**
 * 车辆领域工厂类
 *
 * @author hwyz_leo
 */
@Component
public class VehicleFactory {

    /**
     * 创建车辆领域对象
     *
     * @param vehicleExService 对外服务车辆信息
     * @return 车辆领域对象
     */
    public VehicleDo buildVehicle(VehicleExService vehicleExService) {
        VehicleDo vehicleDo = VehicleDo.builder()
                .id(vehicleExService.getVin())
                .build();
        vehicleDo.init();
        return vehicleDo;
    }

    /**
     * 创建车辆领域对象
     *
     * @param vehStatus 车辆状态
     * @return 车辆领域对象
     */
    public VehicleDo buildVehicle(VehStatusPo vehStatus) {
        VehicleDo vehicleDo = VehicleDo.builder()
                .id(vehStatus.getVin())
                .build();
        vehicleDo.init();
        return vehicleDo;
    }

}
