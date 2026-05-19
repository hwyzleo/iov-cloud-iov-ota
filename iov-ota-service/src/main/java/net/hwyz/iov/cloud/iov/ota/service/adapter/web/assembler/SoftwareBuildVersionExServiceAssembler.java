package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务软件内部版本信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionExServiceAssembler {

    SoftwareBuildVersionExServiceAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param softwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SoftwareBuildVersionExService fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param softwareBuildVersionExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwareBuildVersionPo toPo(SoftwareBuildVersionExService softwareBuildVersionExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SoftwareBuildVersionExService> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
