package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionActivePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;

/**
 * 活动执行占位表 DAO（CR-012 RD-012-5）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionActiveMapper {

    @Select("SELECT * FROM tb_task_vehicle_execution_active WHERE vehicle_task_id = #{vehicleTaskId}")
    ExecutionActivePo selectByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);

    @Insert("INSERT INTO tb_task_vehicle_execution_active (vehicle_task_id, execution_id, create_time) VALUES (#{vehicleTaskId}, #{executionId}, now())")
    int insert(ExecutionActivePo po);

    @Delete("DELETE FROM tb_task_vehicle_execution_active WHERE vehicle_task_id = #{vehicleTaskId}")
    int deleteByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);
}
