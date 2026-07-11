package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.BaselineMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.BaselinePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基线转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaselineMptAssembler {

    BaselineMptAssembler INSTANCE = Mappers.getMapper(BaselineMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param baselinePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BaselineMpt fromPo(BaselinePo baselinePo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param baselinePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaselineMpt> fromPoList(List<BaselinePo> baselinePoList);

}
