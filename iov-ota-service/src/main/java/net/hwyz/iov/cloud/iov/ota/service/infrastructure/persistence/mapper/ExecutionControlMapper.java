package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 安装执行控制指令 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionControlMapper extends BaseMapper<ExecutionControlPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution_control WHERE execution_id = #{executionId} AND row_valid = 1 ORDER BY control_revision DESC LIMIT 1")
    ExecutionControlPo selectLatestByExecutionId(@Param("executionId") Long executionId);
}
