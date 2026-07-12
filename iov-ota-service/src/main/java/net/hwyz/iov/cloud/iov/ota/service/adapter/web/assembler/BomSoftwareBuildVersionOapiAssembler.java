package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwareBuildVersionOapi;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 开放平台BOM软件零件版本转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BomSoftwareBuildVersionOapiAssembler {

    BomSoftwareBuildVersionOapiAssembler INSTANCE = Mappers.getMapper(BomSoftwareBuildVersionOapiAssembler.class);

    /**
     * 数据对象转数据传输对象
     */
    @Mappings({
            @Mapping(target = "softwareDesc", source = "changeNote"),
            @Mapping(target = "releaseDate", source = "releaseTime"),
            @Mapping(target = "softwareReport", ignore = true),
            @Mapping(target = "adaptiveHardwarePn", ignore = true),
            @Mapping(target = "adaptiveSoftwarePn", ignore = true)
    })
    BomSoftwareBuildVersionOapi fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     * BOM 向后兼容：softwareDesc -> changeNote，忽略已删除字段，buildState 默认 DRAFT，releaseDate -> releaseTime
     */
    @Mappings({
            @Mapping(target = "changeNote", source = "softwareDesc"),
            @Mapping(target = "buildState", constant = "DRAFT"),
            @Mapping(target = "releaseTime", source = "releaseDate")
    })
    SoftwareBuildVersionPo toPo(BomSoftwareBuildVersionOapi bomSoftwareBuildVersionOapi);

    /**
     * 数据对象列表转数据传输对象列表
     */
    List<BomSoftwareBuildVersionOapi> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
