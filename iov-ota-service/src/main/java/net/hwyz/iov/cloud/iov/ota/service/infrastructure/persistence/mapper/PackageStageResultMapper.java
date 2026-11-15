package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageStageResultPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 包阶段结果 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface PackageStageResultMapper extends BaseMapper<PackageStageResultPo> {

    @Select("SELECT * FROM tb_package_stage_result WHERE stage_result_id = #{stageResultId} AND row_valid = 1")
    PackageStageResultPo selectByStageResultId(@Param("stageResultId") String stageResultId);

    /**
     * 按（车辆任务, 阶段结果ID）查重：幂等键必须按车辆任务隔离。
     *
     * <p>车端 {@code stage_result_id} 由「包身份 + 结果内容」确定性派生，车辆任务重置后新一轮
     * 会复用同一 ID；若只按全局 {@code stage_result_id} 判重，新车辆任务的阶段结果会被误判
     * 重复而全部丢弃（实车 2026-11-15：重置后新车辆任务没有任何阶段结果，永远停在
     * DOWNLOAD_PENDING，安装许可被永久拒绝）。
     */
    @Select("SELECT * FROM tb_package_stage_result WHERE vehicle_task_id = #{vehicleTaskId} "
            + "AND stage_result_id = #{stageResultId} AND row_valid = 1")
    PackageStageResultPo selectByVehicleTaskIdAndStageResultId(
            @Param("vehicleTaskId") Long vehicleTaskId,
            @Param("stageResultId") String stageResultId);

    @Select("SELECT * FROM tb_package_stage_result WHERE vehicle_task_id = #{vehicleTaskId} AND row_valid = 1")
    List<PackageStageResultPo> selectByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);
}
