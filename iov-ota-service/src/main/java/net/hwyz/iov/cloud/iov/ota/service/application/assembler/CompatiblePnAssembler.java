package net.hwyz.iov.cloud.iov.ota.service.application.assembler;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.CompatiblePnDto;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 兼件号Application层Assembler
 */
@Mapper
public interface CompatiblePnAssembler {

    CompatiblePnAssembler INSTANCE = Mappers.getMapper(CompatiblePnAssembler.class);

    CompatiblePnDto toDto(CompatiblePn domain);

    List<CompatiblePnDto> toDtoList(List<CompatiblePn> domainList);

    CompatiblePn toDomain(CompatiblePnDto dto);
}