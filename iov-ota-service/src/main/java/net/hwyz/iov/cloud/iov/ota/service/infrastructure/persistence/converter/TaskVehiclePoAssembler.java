package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 升级任务车辆数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskVehiclePoAssembler {

    TaskVehiclePoAssembler INSTANCE = Mappers.getMapper(TaskVehiclePoAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param taskVehiclePo 数据对象
     * @return 领域对象
     */
    @Mappings({
            @Mapping(target = "state", ignore = true)
    })
    TaskVehicleDo toDo(TaskVehiclePo taskVehiclePo);

    /**
     * 领域对象转数据对象
     *
     * @param taskVehicleDo 领域对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "state", ignore = true)
    })
    TaskVehiclePo fromDo(TaskVehicleDo taskVehicleDo);

}
