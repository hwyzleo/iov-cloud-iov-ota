package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ConfigWordVo;
import net.hwyz.iov.cloud.ota.pota.api.contract.ConfigWordExService;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务配置字信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigWordExServiceAssembler {

    ConfigWordExServiceAssembler INSTANCE = Mappers.getMapper(ConfigWordExServiceAssembler.class);

    /**
     * 数据传输对象转值对象
     *
     * @param configWordExService 数据传输对象
     * @return 值对象
     */
    @Mappings({})
    ConfigWordVo toVo(ConfigWordExService configWordExService);

    /**
     * 数据传输对象列表转值对象列表
     *
     * @param configWordExServiceList 数据传输对象列表
     * @return 值对象列表
     */
    List<ConfigWordVo> toVoList(List<ConfigWordExService> configWordExServiceList);

}
