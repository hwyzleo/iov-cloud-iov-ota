package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 车辆任务 Mapper（CR-012）
 *
 * <p>独立于旧 TaskVehicleMapper，使用 MyBatis-Plus BaseMapper 自动映射 CR-012 新增列。
 * 两者映射同一张表 tb_task_vehicle。
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskMapper extends BaseMapper<TaskVehiclePo> {

    @Select("SELECT * FROM tb_task_vehicle WHERE task_id = #{taskId} AND vin = #{vin} AND row_valid = 1")
    TaskVehiclePo selectByTaskIdAndVin(@Param("taskId") Long taskId, @Param("vin") String vin);

    @Select("SELECT * FROM tb_task_vehicle WHERE vin = #{vin} AND row_valid = 1 AND vehicle_task_status IS NOT NULL")
    List<TaskVehiclePo> selectByVin(@Param("vin") String vin);

    @Select("SELECT * FROM tb_task_vehicle WHERE task_id = #{taskId} AND row_valid = 1 AND vehicle_task_status IS NOT NULL")
    List<TaskVehiclePo> selectByTaskId(@Param("taskId") Long taskId);
}
