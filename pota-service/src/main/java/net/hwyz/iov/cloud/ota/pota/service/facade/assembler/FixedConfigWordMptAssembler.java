package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordMpt;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.FixedConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台固定配置字转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FixedConfigWordMptAssembler {

    FixedConfigWordMptAssembler INSTANCE = Mappers.getMapper(FixedConfigWordMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param fixedConfigWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    FixedConfigWordMpt fromPo(FixedConfigWordPo fixedConfigWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param fixedConfigWordMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "description", source = "description")
    })
    FixedConfigWordPo toPo(FixedConfigWordMpt fixedConfigWordMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param fixedConfigWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FixedConfigWordMpt> fromPoList(List<FixedConfigWordPo> fixedConfigWordPoList);

}
