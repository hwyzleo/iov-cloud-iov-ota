package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件内部版本Domain Model ⇄ Po转换器
 */
@Mapper(imports = {SoftwareBuildVersionId.class, DeviceCode.class, SoftwarePn.class})
public interface SoftwareBuildVersionConverter {
    
    SoftwareBuildVersionConverter INSTANCE = Mappers.getMapper(SoftwareBuildVersionConverter.class);
    
    @Mapping(target = "id", expression = "java(new SoftwareBuildVersionId(po.getId()))")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(po.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(po.getSoftwarePn()))")
    SoftwareBuildVersion toDomain(SoftwareBuildVersionPo po);
    
    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwareBuildVersionPo toPo(SoftwareBuildVersion domain);
    
    List<SoftwareBuildVersion> toDomainList(List<SoftwareBuildVersionPo> poList);
    
    List<SoftwareBuildVersionPo> toPoList(List<SoftwareBuildVersion> domainList);
}
