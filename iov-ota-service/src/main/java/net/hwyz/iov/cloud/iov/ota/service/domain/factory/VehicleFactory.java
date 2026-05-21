package net.hwyz.iov.cloud.iov.ota.service.domain.factory;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
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
    public VehicleDo buildVehicle(VehicleExResponse vehicleExService) {
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
