package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.BomSoftwareBuildVersionDependencyOapi;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 开放平台BOM软件内部版本依赖转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BomSoftwareBuildVersionDependencyOapiAssembler {

    BomSoftwareBuildVersionDependencyOapiAssembler INSTANCE = Mappers.getMapper(BomSoftwareBuildVersionDependencyOapiAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param softwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BomSoftwareBuildVersionDependencyOapi fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param bomSoftwareBuildVersionDependencyOapi 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwareBuildVersionPo toPo(BomSoftwareBuildVersionDependencyOapi bomSoftwareBuildVersionDependencyOapi);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BomSoftwareBuildVersionDependencyOapi> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
