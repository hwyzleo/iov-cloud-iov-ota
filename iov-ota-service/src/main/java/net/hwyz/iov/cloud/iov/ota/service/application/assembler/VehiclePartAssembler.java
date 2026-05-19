package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件Application层Assembler
 */
@Mapper
public interface VehiclePartAssembler {

    VehiclePartAssembler INSTANCE = Mappers.getMapper(VehiclePartAssembler.class);

    VehiclePartDto toDto(VehiclePart domain);

    List<VehiclePartDto> toDtoList(List<VehiclePart> domainList);

    VehiclePart toDomain(VehiclePartDto dto);
}