package net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.assembler;

import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 升级任务数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskPoAssembler {

    TaskPoAssembler INSTANCE = Mappers.getMapper(TaskPoAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param taskPo 数据对象
     * @return 领域对象
     */
    @Mappings({
            @Mapping(target = "type", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType.valOf(taskPo.getType()))"),
            @Mapping(target = "phase", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase.valOf(taskPo.getPhase()))"),
            @Mapping(target = "upgradeMode", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode.valOf(taskPo.getUpgradeMode()))"),
            @Mapping(target = "upgradeModeArg", expression = "java(net.hwyz.iov.cloud.framework.common.util.AssemblerHelper.string2Json(taskPo.getUpgradeModeArg()))"),
            @Mapping(target = "taskState", expression = "java(net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState.valOf(taskPo.getState()))"),
            @Mapping(target = "state", ignore = true)
    })
    TaskDo toDo(TaskPo taskPo);

    /**
     * 领域对象转数据对象
     *
     * @param taskDo 领域对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "type", source = "type.value"),
            @Mapping(target = "phase", source = "phase.value"),
            @Mapping(target = "upgradeMode", source = "upgradeMode.value"),
            @Mapping(target = "upgradeModeArg", expression = "java(net.hwyz.iov.cloud.framework.common.util.AssemblerHelper.json2String(taskDo.getUpgradeModeArg()))"),
            @Mapping(target = "state", source = "taskState.value"),
            @Mapping(target = "description", source = "description")
    })
    TaskPo fromDo(TaskDo taskDo);

}
