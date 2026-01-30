package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.BomConfigWordDependencyOapi;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 开放平台BOM配置字依赖转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BomConfigWordDependencyOapiAssembler {

    BomConfigWordDependencyOapiAssembler INSTANCE = Mappers.getMapper(BomConfigWordDependencyOapiAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BomConfigWordDependencyOapi fromPo(ConfigWordPo configWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param bomConfigWordDependencyOapi 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ConfigWordPo toPo(BomConfigWordDependencyOapi bomConfigWordDependencyOapi);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BomConfigWordDependencyOapi> fromPoList(List<ConfigWordPo> configWordPoList);

}
