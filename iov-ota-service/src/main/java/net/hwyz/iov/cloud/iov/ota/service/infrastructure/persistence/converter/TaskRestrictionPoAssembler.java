package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskRestrictionVo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 升级任务限制数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskRestrictionPoAssembler {

    TaskRestrictionPoAssembler INSTANCE = Mappers.getMapper(TaskRestrictionPoAssembler.class);

    /**
     * 数据对象转值对象
     *
     * @param taskRestrictionPo 数据对象
     * @return 值对象
     */
    @Mappings({})
    TaskRestrictionVo toVo(TaskRestrictionPo taskRestrictionPo);

    /**
     * 值对象转数据对象
     *
     * @param taskRestrictionVo 值对象
     * @return 数据对象
     */
    @Mappings({})
    TaskRestrictionPo fromVo(TaskRestrictionVo taskRestrictionVo);

    /**
     * 数据对象列表转值对象列表
     *
     * @param taskRestrictionPoList 数据对象列表
     * @return 值对象列表
     */
    List<TaskRestrictionVo> toVoList(List<TaskRestrictionPo> taskRestrictionPoList);

}
