package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    /**
     * 任务授权状态分布聚合（CR-016 §8）。
     */
    @Select("SELECT consent_state AS state, COUNT(*) AS cnt FROM tb_task_vehicle " +
            "WHERE task_id = #{taskId} AND row_valid = 1 GROUP BY consent_state")
    List<Map<String, Object>> countConsentStateByTask(@Param("taskId") Long taskId);

    /**
     * 乐观锁推进 VehicleTask 当前授权状态（CR-016 §3.1/§3.3）。
     *
     * <p>同一事务内由 ConsentAppService 调用：WHERE 带 row_version 校验，
     * 返回 0 表示并发冲突（其他消费者已推进）。
     */
    @Update("""
            UPDATE tb_task_vehicle
            SET vehicle_task_status   = #{vehicleTaskStatus},
                download_ready_state = #{downloadReadyState},
                consent_state        = #{consentState},
                current_consent_id   = #{currentConsentId},
                consent_scope_digest = #{consentScopeDigest},
                consent_updated_at   = #{consentUpdatedAt},
                row_version          = row_version + 1,
                modify_time          = NOW(3)
            WHERE id = #{id} AND row_version = #{expectedRowVersion} AND row_valid = 1
            """)
    int updateCurrentConsent(@Param("id") Long id,
                             @Param("expectedRowVersion") long expectedRowVersion,
                             @Param("vehicleTaskStatus") String vehicleTaskStatus,
                             @Param("downloadReadyState") String downloadReadyState,
                             @Param("consentState") String consentState,
                             @Param("currentConsentId") Long currentConsentId,
                             @Param("consentScopeDigest") String consentScopeDigest,
                             @Param("consentUpdatedAt") Date consentUpdatedAt);
}
