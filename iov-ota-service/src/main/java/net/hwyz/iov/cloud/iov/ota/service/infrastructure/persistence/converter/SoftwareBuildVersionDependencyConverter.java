package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionDependency;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 软件内部版本依赖Domain Model ⇄ Po转换器
 */
@Mapper(componentModel = "spring", imports = {SoftwareBuildVersionId.class})
public interface SoftwareBuildVersionDependencyConverter {
    
    @Mapping(target = "softwareBuildVersionId", expression = "java(new SoftwareBuildVersionId(po.getSoftwareBuildVersionId()))")
    @Mapping(target = "dependencySoftwareBuildVersionId", expression = "java(new SoftwareBuildVersionId(po.getDependencySoftwareBuildVersionId()))")
    SoftwareBuildVersionDependency toDomain(SoftwareBuildVersionDependencyPo po);
    
    @Mapping(target = "softwareBuildVersionId", expression = "java(domain.getSoftwareBuildVersionId() != null ? domain.getSoftwareBuildVersionId().getValue() : null)")
    @Mapping(target = "dependencySoftwareBuildVersionId", expression = "java(domain.getDependencySoftwareBuildVersionId() != null ? domain.getDependencySoftwareBuildVersionId().getValue() : null)")
    SoftwareBuildVersionDependencyPo toPo(SoftwareBuildVersionDependency domain);
    
    List<SoftwareBuildVersionDependency> toDomainList(List<SoftwareBuildVersionDependencyPo> poList);
    
    List<SoftwareBuildVersionDependencyPo> toPoList(List<SoftwareBuildVersionDependency> domainList);
}
