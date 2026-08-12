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

    @Select("SELECT * FROM tb_package_stage_result WHERE vehicle_task_id = #{vehicleTaskId} AND row_valid = 1")
    List<PackageStageResultPo> selectByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);
}
