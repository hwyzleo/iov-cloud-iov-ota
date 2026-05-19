package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件包Domain Model ⇄ Po转换器
 */
@Mapper(imports = {SoftwarePackageId.class, DeviceCode.class, SoftwarePn.class})
public interface SoftwarePackageConverter {
    
    SoftwarePackageConverter INSTANCE = Mappers.getMapper(SoftwarePackageConverter.class);
    
    @Mapping(target = "id", expression = "java(new SoftwarePackageId(po.getId()))")
    @Mapping(target = "deviceCode", expression = "java(new DeviceCode(po.getDeviceCode()))")
    @Mapping(target = "softwarePn", expression = "java(new SoftwarePn(po.getSoftwarePn()))")
    SoftwarePackage toDomain(SoftwarePackagePo po);
    
    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwarePackagePo toPo(SoftwarePackage domain);
    
    List<SoftwarePackage> toDomainList(List<SoftwarePackagePo> poList);
    
    List<SoftwarePackagePo> toPoList(List<SoftwarePackage> domainList);
}
