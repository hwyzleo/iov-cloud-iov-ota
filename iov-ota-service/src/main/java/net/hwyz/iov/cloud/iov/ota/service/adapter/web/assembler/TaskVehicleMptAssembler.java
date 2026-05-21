package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.TaskVehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehiclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆升级任务转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskVehicleMptAssembler {

    TaskVehicleMptAssembler INSTANCE = Mappers.getMapper(TaskVehicleMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param taskVehiclePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    TaskVehicleMpt fromPo(TaskVehiclePo taskVehiclePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param taskVehicleMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    TaskVehiclePo toPo(TaskVehicleMpt taskVehicleMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param taskVehiclePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<TaskVehicleMpt> fromPoList(List<TaskVehiclePo> taskVehiclePoList);

}
