package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.ota.baseline.api.contract.BaselineSoftwareBuildVersionExService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivitySoftwareBuildVersionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基线下软件内部版本转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaselineSoftwareBuildVersionExServiceAssembler {

    BaselineSoftwareBuildVersionExServiceAssembler INSTANCE = Mappers.getMapper(BaselineSoftwareBuildVersionExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param activitySoftwareBuildVersionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BaselineSoftwareBuildVersionExService fromPo(ActivitySoftwareBuildVersionPo activitySoftwareBuildVersionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param baselineSoftwareBuildVersionExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ActivitySoftwareBuildVersionPo toPo(BaselineSoftwareBuildVersionExService baselineSoftwareBuildVersionExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param activitySoftwareBuildVersionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaselineSoftwareBuildVersionExService> fromPoList(List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionPoList);

    /**
     * 数据传输对象列表转数据对象列表
     *
     * @param baselineSoftwareBuildVersionExServiceList 数据传输对象列表
     * @return 数据对象列表
     */
    List<ActivitySoftwareBuildVersionPo> toPoList(List<BaselineSoftwareBuildVersionExService> baselineSoftwareBuildVersionExServiceList);

}
