package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivitySoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivitySoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级活动下软件内部版本转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivitySoftwareBuildVersionMptAssembler {

    ActivitySoftwareBuildVersionMptAssembler INSTANCE = Mappers.getMapper(ActivitySoftwareBuildVersionMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param activitySoftwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ActivitySoftwareBuildVersionMpt fromPo(ActivitySoftwareBuildVersionPo activitySoftwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param activitySoftwareBuildVersionMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ActivitySoftwareBuildVersionPo toPo(ActivitySoftwareBuildVersionMpt activitySoftwareBuildVersionMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param activitySoftwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ActivitySoftwareBuildVersionMpt> fromPoList(List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionPoList);

}
