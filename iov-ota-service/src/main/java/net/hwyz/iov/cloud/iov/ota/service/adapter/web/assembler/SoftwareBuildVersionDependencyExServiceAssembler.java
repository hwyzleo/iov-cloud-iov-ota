package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionDependencyExService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionDependencyVo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionDependencyPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务软件内部版本依赖信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionDependencyExServiceAssembler {

    SoftwareBuildVersionDependencyExServiceAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionDependencyExServiceAssembler.class);

    /**
     * 数据传输对象转值对象
     *
     * @param softwareBuildVersionDependencyExService 数据传输对象
     * @return 值对象
     */
    @Mappings({
            @Mapping(target = "adaptiveLevel", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.AdaptiveLevel.valOf(softwareBuildVersionDependencyExService.getAdaptiveLevel()))")
    })
    SoftwareBuildVersionDependencyVo toVo(SoftwareBuildVersionDependencyExService softwareBuildVersionDependencyExService);

    /**
     * 数据传输对象列表转值对象列表
     *
     * @param softwareBuildVersionDependencyExServiceList 数据传输对象列表
     * @return 值对象列表
     */
    List<SoftwareBuildVersionDependencyVo> toVoList(List<SoftwareBuildVersionDependencyExService> softwareBuildVersionDependencyExServiceList);

    @Mappings({})
    SoftwareBuildVersionDependencyExService fromPo(SoftwareBuildVersionDependencyPo softwareBuildVersionDependencyPo);

    List<SoftwareBuildVersionDependencyExService> fromPoList(List<SoftwareBuildVersionDependencyPo> softwareBuildVersionDependencyPoList);

}
