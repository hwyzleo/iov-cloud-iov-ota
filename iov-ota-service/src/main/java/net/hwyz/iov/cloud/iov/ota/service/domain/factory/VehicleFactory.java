package net.hwyz.iov.cloud.iov.ota.service.domain.factory;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleExResponse;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
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

    /**
     * 从车辆投影创建车辆领域对象
     * <p>
     * CR-011: 支持从 tb_vehicle_projection 构建
     * </p>
     *
     * @param vehicleProjection 车辆投影
     * @return 车辆领域对象
     */
    public VehicleDo buildVehicleFromProjection(VehicleProjectionPo vehicleProjection) {
        VehicleDo vehicleDo = VehicleDo.builder()
                .id(vehicleProjection.getVin())
                .build();
        vehicleDo.init();
        return vehicleDo;
    }

}
