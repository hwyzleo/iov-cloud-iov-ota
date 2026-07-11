package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ConfigWordProfileMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordProfilePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置字配置文件转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigWordProfileMptAssembler {

    ConfigWordProfileMptAssembler INSTANCE = Mappers.getMapper(ConfigWordProfileMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configWordProfilePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordProfileMpt fromPo(ConfigWordProfilePo configWordProfilePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configWordProfileMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    ConfigWordProfilePo toPo(ConfigWordProfileMpt configWordProfileMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configWordProfilePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigWordProfileMpt> fromPoList(List<ConfigWordProfilePo> configWordProfilePoList);

}
