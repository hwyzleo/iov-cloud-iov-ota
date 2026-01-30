package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.FixedConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务固定配置字转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FixedConfigWordExServiceAssembler {

    FixedConfigWordExServiceAssembler INSTANCE = Mappers.getMapper(FixedConfigWordExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param fixedConfigWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    FixedConfigWordExService fromPo(FixedConfigWordPo fixedConfigWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param fixedConfigWordExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    FixedConfigWordPo toPo(FixedConfigWordExService fixedConfigWordExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param fixedConfigWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FixedConfigWordExService> fromPoList(List<FixedConfigWordPo> fixedConfigWordPoList);

}
