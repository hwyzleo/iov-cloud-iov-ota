package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleResetLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 车辆任务重置审计 Mapper（IOV-OTA-DSN-CR-025 §4.29.4）。
 *
 * <p>审计表为不可变追加写，不提供更新/删除入口。</p>
 *
 * @author hwyz_leo
 */
@Mapper
public interface TaskVehicleResetLogMapper extends BaseMapper<TaskVehicleResetLogPo> {

    /**
     * 单车辆任务重置次数（按原车辆任务归属统计）。
     */
    @Select("SELECT COUNT(*) FROM tb_task_vehicle_reset_log "
            + "WHERE old_vehicle_task_id = #{vehicleTaskId} AND row_valid = 1")
    int countByVehicleTaskId(@Param("vehicleTaskId") Long vehicleTaskId);

    /**
     * 批量统计 (task_id, vin) 维度的重置次数，供列表页避免 N+1。
     *
     * <p>返回列：task_id / vin / cnt。调用方按 (taskId, vin) 组装。</p>
     */
    @Select("<script>"
            + "SELECT task_id AS task_id, vin AS vin, COUNT(*) AS cnt FROM tb_task_vehicle_reset_log "
            + "WHERE row_valid = 1 AND task_id IN "
            + "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>#{taskId}</foreach> "
            + "GROUP BY task_id, vin"
            + "</script>")
    List<Map<String, Object>> countByTaskIdsGroupByVin(@Param("taskIds") List<Long> taskIds);
}
