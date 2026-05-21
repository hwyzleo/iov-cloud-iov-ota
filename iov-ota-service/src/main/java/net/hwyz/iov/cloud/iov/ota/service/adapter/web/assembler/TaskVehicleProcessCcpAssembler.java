package net.hwyz.iov.cloud.iov.ota.service.facade.assembler;

import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleProcessCcp;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleProcessPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 中央计算平台车辆升级任务升级过程转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskVehicleProcessCcpAssembler {

    TaskVehicleProcessCcpAssembler INSTANCE = Mappers.getMapper(TaskVehicleProcessCcpAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param taskVehicleProcessPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    TaskVehicleProcessCcp fromPo(TaskVehicleProcessPo taskVehicleProcessPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param taskVehicleProcessCcp 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    TaskVehicleProcessPo toPo(TaskVehicleProcessCcp taskVehicleProcessCcp);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param taskVehicleProcessPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<TaskVehicleProcessCcp> fromPoList(List<TaskVehicleProcessPo> taskVehicleProcessPoList);

}
