package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.CreateSoftwareBuildVersionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.SoftwareBuildVersionQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.SoftwareBuildVersionDto;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台软件内部版本信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionMptAssembler {

    SoftwareBuildVersionMptAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionMptAssembler.class);

    @Mappings({})
    SoftwareBuildVersionMpt fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    @Mappings({})
    SoftwareBuildVersionPo toPo(SoftwareBuildVersionMpt softwareBuildVersionMpt);

    List<SoftwareBuildVersionMpt> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

    SoftwareBuildVersionMpt fromDto(SoftwareBuildVersionDto dto);

    List<SoftwareBuildVersionMpt> fromDtoList(List<SoftwareBuildVersionDto> dtoList);

    @Mappings({})
    CreateSoftwareBuildVersionCmd toCmd(SoftwareBuildVersionMpt mpt);

    @Mapping(target = "baselineCode", ignore = true)
    @Mapping(target = "beginTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    SoftwareBuildVersionQuery toQuery(SoftwareBuildVersionMpt mpt);
}
