package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件Domain Model ⇄ Po转换器
 */
@Mapper
public interface VehiclePartConverter {
    
    VehiclePartConverter INSTANCE = Mappers.getMapper(VehiclePartConverter.class);
    
    VehiclePart toDomain(VehiclePartPo po);
    
    VehiclePartPo toPo(VehiclePart domain);
    
    List<VehiclePart> toDomainList(List<VehiclePartPo> poList);
    
    List<VehiclePartPo> toPoList(List<VehiclePart> domainList);
}
