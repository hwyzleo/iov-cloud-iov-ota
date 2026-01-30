package net.hwyz.iov.cloud.ota.pota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台软件内部版本信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionMptAssembler {

    SoftwareBuildVersionMptAssembler INSTANCE = Mappers.getMapper(SoftwareBuildVersionMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param softwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SoftwareBuildVersionMpt fromPo(SoftwareBuildVersionPo softwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param softwareBuildVersionMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SoftwareBuildVersionPo toPo(SoftwareBuildVersionMpt softwareBuildVersionMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param softwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SoftwareBuildVersionMpt> fromPoList(List<SoftwareBuildVersionPo> softwareBuildVersionPoList);

}
