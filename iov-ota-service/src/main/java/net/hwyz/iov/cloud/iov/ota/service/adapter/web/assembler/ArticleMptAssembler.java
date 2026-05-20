package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.api.contract.ArticleMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ArticlePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台文章转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ArticleMptAssembler {

    ArticleMptAssembler INSTANCE = Mappers.getMapper(ArticleMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param articlePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ArticleMpt fromPo(ArticlePo articlePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param articleMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ArticlePo toPo(ArticleMpt articleMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param articlePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ArticleMpt> fromPoList(List<ArticlePo> articlePoList);

}
