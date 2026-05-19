package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 车辆零件Domain Model ⇄ Po转换器
 */
@Mapper(componentModel = "spring")
public interface VehiclePartConverter {
    
    VehiclePart toDomain(VehiclePartPo po);
    
    VehiclePartPo toPo(VehiclePart domain);
    
    List<VehiclePart> toDomainList(List<VehiclePartPo> poList);
    
    List<VehiclePartPo> toPoList(List<VehiclePart> domainList);
}
