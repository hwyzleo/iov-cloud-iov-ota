package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 车辆任务包快照 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskPackageMapper extends BaseMapper<VehicleTaskPackagePo> {

    @Select("SELECT * FROM tb_vehicle_task_package WHERE vehicle_task_id = #{vehicleTaskId} AND row_valid = 1")
    List<VehicleTaskPackagePo> selectByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);
}
