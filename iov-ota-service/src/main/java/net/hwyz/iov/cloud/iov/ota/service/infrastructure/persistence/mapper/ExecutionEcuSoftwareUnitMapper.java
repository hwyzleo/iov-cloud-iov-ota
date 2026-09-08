package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEcuSoftwareUnitPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 安装执行 ECU 软件单元结果 DAO（CR-019 §4.23.5）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionEcuSoftwareUnitMapper extends BaseMapper<ExecutionEcuSoftwareUnitPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution_ecu_software_unit WHERE ecu_result_id = #{ecuResultId} AND row_valid = 1 "
            + "ORDER BY software_target_code, slot")
    List<ExecutionEcuSoftwareUnitPo> selectByEcuResultId(@Param("ecuResultId") Long ecuResultId);

    @Select("SELECT * FROM tb_task_vehicle_execution_ecu_software_unit WHERE execution_id = #{executionId} AND row_valid = 1 "
            + "ORDER BY ecu_id, software_target_code, slot")
    List<ExecutionEcuSoftwareUnitPo> selectByExecutionId(@Param("executionId") Long executionId);
}
