package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.VehiclePartExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车辆零件信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartExServiceAssembler {

    VehiclePartExServiceAssembler INSTANCE = Mappers.getMapper(VehiclePartExServiceAssembler.class);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehiclePartExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehiclePartPo toPo(VehiclePartExService vehiclePartExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehiclePartExServiceList 数据传输对象列表
     * @return 数据对象列表
     */
    List<VehiclePartPo> toPoList(List<VehiclePartExService> vehiclePartExServiceList);

}
