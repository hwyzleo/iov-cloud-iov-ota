package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.api.contract.ActivityFixedConfigWordMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityFixedConfigWordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级活动下固定配置字转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivityFixedConfigWordMptAssembler {

    ActivityFixedConfigWordMptAssembler INSTANCE = Mappers.getMapper(ActivityFixedConfigWordMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param activityFixedConfigWordPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ActivityFixedConfigWordMpt fromPo(ActivityFixedConfigWordPo activityFixedConfigWordPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param activityFixedConfigWordMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ActivityFixedConfigWordPo toPo(ActivityFixedConfigWordMpt activityFixedConfigWordMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param activityFixedConfigWordPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ActivityFixedConfigWordMpt> fromPoList(List<ActivityFixedConfigWordPo> activityFixedConfigWordPoList);

}
