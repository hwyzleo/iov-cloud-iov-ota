package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwareBuildVersionVo;
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

    @Mappings({})
    SoftwareBuildVersionExService fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    List<SoftwareBuildVersionExService> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
