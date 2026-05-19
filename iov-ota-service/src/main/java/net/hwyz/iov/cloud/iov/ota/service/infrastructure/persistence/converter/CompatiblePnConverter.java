package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 兼件号Domain Model ⇄ Po转换器
 */
@Mapper(componentModel = "spring")
public interface CompatiblePnConverter {
    
    CompatiblePn toDomain(CompatiblePnPo po);
    
    CompatiblePnPo toPo(CompatiblePn domain);
    
    List<CompatiblePn> toDomainList(List<CompatiblePnPo> poList);
    
    List<CompatiblePnPo> toPoList(List<CompatiblePn> domainList);
}
