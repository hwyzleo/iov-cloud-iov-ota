package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.SoftwareBuildVersionVo;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionExService;
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
     * 数据传输对象转值对象
     *
     * @param softwareBuildVersionExService 数据传输对象
     * @return 值对象
     */
    @Mappings({})
    SoftwareBuildVersionVo toVo(SoftwareBuildVersionExService softwareBuildVersionExService);

    /**
     * 数据传输对象列表转值对象列表
     *
     * @param softwareBuildVersionExServiceList 数据传输对象列表
     * @return 值对象列表
     */
    List<SoftwareBuildVersionVo> toVoList(List<SoftwareBuildVersionExService> softwareBuildVersionExServiceList);

}
