package net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ota.api.vo.TaskMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台升级任务转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskMptAssembler {

    TaskMptAssembler INSTANCE = Mappers.getMapper(TaskMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param taskPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    TaskMpt fromPo(TaskPo taskPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param taskMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    TaskPo toPo(TaskMpt taskMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param taskPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<TaskMpt> fromPoList(List<TaskPo> taskPoList);

}
