package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEcuResultPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 安装执行 ECU 结果 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionEcuResultMapper extends BaseMapper<ExecutionEcuResultPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution_ecu_result WHERE execution_id = #{executionId} AND row_valid = 1")
    List<ExecutionEcuResultPo> selectByExecutionId(@Param("executionId") Long executionId);
}
