package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.CompatiblePnPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务兼容零件号转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface CompatiblePnExServiceAssembler {

    CompatiblePnExServiceAssembler INSTANCE = Mappers.getMapper(CompatiblePnExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param compatiblePnPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    CompatiblePnExService fromPo(CompatiblePnPo compatiblePnPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param compatiblePnExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    CompatiblePnPo toPo(CompatiblePnExService compatiblePnExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param compatiblePnPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<CompatiblePnExService> fromPoList(List<CompatiblePnPo> compatiblePnPoList);

}
