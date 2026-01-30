package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionDependencyExService;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionDependencyPo;
import org.mapstruct.Mapper;
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
     * 数据对象转数据传输对象
     *
     * @param softwareBuildVersionDependencyPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SoftwareBuildVersionDependencyExService fromPo(SoftwareBuildVersionDependencyPo softwareBuildVersionDependencyPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param softwareBuildVersionDependencyExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwareBuildVersionDependencyPo toPo(SoftwareBuildVersionDependencyExService softwareBuildVersionDependencyExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwareBuildVersionDependencyPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SoftwareBuildVersionDependencyExService> fromPoList(List<SoftwareBuildVersionDependencyPo> softwareBuildVersionDependencyPoList);

}
