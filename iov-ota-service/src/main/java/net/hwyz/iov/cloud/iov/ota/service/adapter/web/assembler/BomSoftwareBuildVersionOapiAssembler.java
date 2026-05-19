package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.BomSoftwareBuildVersionOapi;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
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
     *
     * @param softwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BomSoftwareBuildVersionOapi fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param bomSoftwareBuildVersionOapi 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwareBuildVersionPo toPo(BomSoftwareBuildVersionOapi bomSoftwareBuildVersionOapi);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BomSoftwareBuildVersionOapi> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
