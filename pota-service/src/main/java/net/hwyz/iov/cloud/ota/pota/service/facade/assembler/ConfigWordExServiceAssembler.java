package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.ConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务配置字转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigWordExServiceAssembler {

    ConfigWordExServiceAssembler INSTANCE = Mappers.getMapper(ConfigWordExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordExService fromPo(ConfigWordPo configWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configWordExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordPo toPo(ConfigWordExService configWordExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigWordExService> fromPoList(List<ConfigWordPo> configWordPoList);

}
