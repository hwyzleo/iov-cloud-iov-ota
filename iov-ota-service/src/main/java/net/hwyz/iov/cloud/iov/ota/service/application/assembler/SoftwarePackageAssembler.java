package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.SoftwarePackageDto;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件包Application层Assembler
 */
@Mapper(imports = {SoftwarePackageId.class, DeviceCode.class, SoftwarePn.class})
public interface SoftwarePackageAssembler {

    SoftwarePackageAssembler INSTANCE = Mappers.getMapper(SoftwarePackageAssembler.class);

    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    SoftwarePackageDto toDto(SoftwarePackage domain);

    List<SoftwarePackageDto> toDtoList(List<SoftwarePackage> domainList);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? new SoftwarePackageId(dto.getId()) : null)")
    @Mapping(target = "deviceCode", expression = "java(dto.getDeviceCode() != null ? new DeviceCode(dto.getDeviceCode()) : null)")
    @Mapping(target = "softwarePn", expression = "java(dto.getSoftwarePn() != null ? new SoftwarePn(dto.getSoftwarePn()) : null)")
    SoftwarePackage toDomain(SoftwarePackageDto dto);
}