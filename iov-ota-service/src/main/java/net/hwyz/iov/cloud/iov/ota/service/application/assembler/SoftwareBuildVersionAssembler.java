package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.CreateSoftwareBuildVersionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ModifySoftwareBuildVersionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.SoftwareBuildVersionDto;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件内部版本Application层Assembler
 */
@Mapper(imports = {SoftwareBuildVersionId.class, DeviceCode.class, SoftwarePn.class})
public interface SoftwareBuildVersionAssembler {

    SoftwareBuildVersionAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionAssembler.class);

    @Mapping(target = "id", expression = "java(cmd.getDeviceCode() != null ? new SoftwareBuildVersionId(null) : null)")
    @Mapping(target = "deviceCode", expression = "java(cmd.getDeviceCode() != null ? new DeviceCode(cmd.getDeviceCode()) : null)")
    @Mapping(target = "softwarePn", expression = "java(cmd.getSoftwarePn() != null ? new SoftwarePn(cmd.getSoftwarePn()) : null)")
    @Mapping(target = "packages", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "buildState", ignore = true)
    @Mapping(target = "releaseTime", ignore = true)
    SoftwareBuildVersion toDomain(CreateSoftwareBuildVersionCmd cmd);

    @Mapping(target = "id", expression = "java(cmd.getId() != null ? new SoftwareBuildVersionId(cmd.getId()) : null)")
    @Mapping(target = "deviceCode", expression = "java(cmd.getDeviceCode() != null ? new DeviceCode(cmd.getDeviceCode()) : null)")
    @Mapping(target = "softwarePn", expression = "java(cmd.getSoftwarePn() != null ? new SoftwarePn(cmd.getSoftwarePn()) : null)")
    @Mapping(target = "packages", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "buildState", ignore = true)
    @Mapping(target = "releaseTime", ignore = true)
    SoftwareBuildVersion toDomain(ModifySoftwareBuildVersionCmd cmd);

    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().getValue() : null)")
    @Mapping(target = "deviceCode", expression = "java(domain.getDeviceCode() != null ? domain.getDeviceCode().getValue() : null)")
    @Mapping(target = "softwarePn", expression = "java(domain.getSoftwarePn() != null ? domain.getSoftwarePn().getValue() : null)")
    @Mapping(target = "softwarePackageCount", ignore = true)
    @Mapping(target = "dependencyCount", ignore = true)
    @Mapping(target = "testReportCount", ignore = true)
    @Mapping(target = "adaptationCount", ignore = true)
    @Mapping(target = "adaptiveLevel", ignore = true)
    SoftwareBuildVersionDto toDto(SoftwareBuildVersion domain);

    List<SoftwareBuildVersionDto> toDtoList(List<SoftwareBuildVersion> domainList);
}