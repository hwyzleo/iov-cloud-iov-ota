package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.ActivityCompatiblePnMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityCompatiblePnPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级活动下兼容零件号转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivityCompatiblePnMptAssembler {

    ActivityCompatiblePnMptAssembler INSTANCE = Mappers.getMapper(ActivityCompatiblePnMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param activityCompatiblePnPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ActivityCompatiblePnMpt fromPo(ActivityCompatiblePnPo activityCompatiblePnPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param activityCompatiblePnMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ActivityCompatiblePnPo toPo(ActivityCompatiblePnMpt activityCompatiblePnMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param activityCompatiblePnPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ActivityCompatiblePnMpt> fromPoList(List<ActivityCompatiblePnPo> activityCompatiblePnPoList);

}
