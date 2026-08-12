package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskSnapshotPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆任务快照 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleTaskSnapshotMapper extends BaseMapper<VehicleTaskSnapshotPo> {

    @Select("SELECT * FROM tb_vehicle_task_snapshot WHERE vehicle_task_id = #{vehicleTaskId} AND task_revision = #{taskRevision} AND row_valid = 1")
    VehicleTaskSnapshotPo selectByVehicleTaskIdAndRevision(@Param("vehicleTaskId") Long vehicleTaskId, @Param("taskRevision") Long taskRevision);
}
