package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级活动转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivityMptAssembler {

    ActivityMptAssembler INSTANCE = Mappers.getMapper(ActivityMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param activityPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ActivityMpt fromPo(ActivityPo activityPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param activityMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ActivityPo toPo(ActivityMpt activityMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param activityPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ActivityMpt> fromPoList(List<ActivityPo> activityPoList);

}
