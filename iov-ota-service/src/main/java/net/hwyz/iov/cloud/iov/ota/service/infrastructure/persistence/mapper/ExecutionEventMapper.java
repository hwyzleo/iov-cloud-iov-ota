package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 安装执行事件 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionEventMapper extends BaseMapper<ExecutionEventPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution_event WHERE execution_id = #{executionId} AND row_valid = 1 ORDER BY sequence_no")
    List<ExecutionEventPo> selectByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT * FROM tb_task_vehicle_execution_event WHERE event_id = #{eventId} AND row_valid = 1")
    ExecutionEventPo selectByEventId(@Param("eventId") String eventId);
}
