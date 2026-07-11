package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ConfigWordFieldMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordFieldPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置字字段转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigWordFieldMptAssembler {

    ConfigWordFieldMptAssembler INSTANCE = Mappers.getMapper(ConfigWordFieldMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configWordFieldPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordFieldMpt fromPo(ConfigWordFieldPo configWordFieldPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configWordFieldMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordFieldPo toPo(ConfigWordFieldMpt configWordFieldMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configWordFieldPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigWordFieldMpt> fromPoList(List<ConfigWordFieldPo> configWordFieldPoList);

}
