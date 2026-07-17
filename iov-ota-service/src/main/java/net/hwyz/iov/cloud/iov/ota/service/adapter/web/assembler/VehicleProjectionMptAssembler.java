package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆投影到管理后台VO转换类
 * <p>
 * CR-011: 支持 VehicleProjectionPo 到 VehicleMpt 的转换
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VehicleProjectionMptAssembler {

    VehicleProjectionMptAssembler INSTANCE = Mappers.getMapper(VehicleProjectionMptAssembler.class);

    /**
     * 车辆投影数据对象转管理后台数据传输对象
     *
     * @param vehicleProjectionPo 车辆投影数据对象
     * @return 管理后台数据传输对象
     */
    VehicleMpt fromPo(VehicleProjectionPo vehicleProjectionPo);

    /**
     * 管理后台数据传输对象转车辆投影数据对象
     *
     * @param vehicleMpt 管理后台数据传输对象
     * @return 车辆投影数据对象
     */
    VehicleProjectionPo toPo(VehicleMpt vehicleMpt);

    /**
     * 车辆投影数据对象列表转管理后台数据传输对象列表
     *
     * @param vehicleProjectionPoList 车辆投影数据对象列表
     * @return 管理后台数据传输对象列表
     */
    List<VehicleMpt> fromPoList(List<VehicleProjectionPo> vehicleProjectionPoList);
}
