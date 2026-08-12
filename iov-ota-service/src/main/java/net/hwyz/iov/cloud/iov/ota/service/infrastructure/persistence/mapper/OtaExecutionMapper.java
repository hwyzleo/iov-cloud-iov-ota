package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 安装执行主表 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface OtaExecutionMapper extends BaseMapper<OtaExecutionPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution WHERE vehicle_task_id = #{vehicleTaskId} AND attempt_no = #{attemptNo} AND row_valid = 1")
    OtaExecutionPo selectByVehicleTaskIdAndAttemptNo(@Param("vehicleTaskId") Long vehicleTaskId, @Param("attemptNo") Integer attemptNo);

    @Select("SELECT * FROM tb_task_vehicle_execution WHERE execution_id = #{executionId} AND row_valid = 1")
    OtaExecutionPo selectByExecutionId(@Param("executionId") String executionId);
}
