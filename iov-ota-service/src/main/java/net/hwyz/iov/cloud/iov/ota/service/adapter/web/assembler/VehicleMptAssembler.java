package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级车辆转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleMptAssembler {

    VehicleMptAssembler INSTANCE = Mappers.getMapper(VehicleMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehStatusPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleMpt fromPo(VehStatusPo vehStatusPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehStatusPo toPo(VehicleMpt vehicleMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehStatusPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleMpt> fromPoList(List<VehStatusPo> vehStatusPoList);

}
