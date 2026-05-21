package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.VehStatusPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆设置数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehStatusPoAssembler {

    VehStatusPoAssembler INSTANCE = Mappers.getMapper(VehStatusPoAssembler.class);

    /**
     * 领域对象转数据对象
     *
     * @param vehicleDo 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VehStatusPo fromDo(VehicleDo vehicleDo);

}
