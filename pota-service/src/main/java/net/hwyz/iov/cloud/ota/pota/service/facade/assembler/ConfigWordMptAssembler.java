package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.ConfigWordMpt;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置字转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigWordMptAssembler {

    ConfigWordMptAssembler INSTANCE = Mappers.getMapper(ConfigWordMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordMpt fromPo(ConfigWordPo configWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configWordMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordPo toPo(ConfigWordMpt configWordMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigWordMpt> fromPoList(List<ConfigWordPo> configWordPoList);

}
