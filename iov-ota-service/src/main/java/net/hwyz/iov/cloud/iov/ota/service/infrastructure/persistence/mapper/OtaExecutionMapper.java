package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OtaExecutionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 收口/时序关键字段显式更新（IOV-OTA-DSN-CR-026 §7.2）。
     *
     * <p>整行 {@code updateById} 在乐观锁版本不匹配或非空更新策略下可能影响 0 行，
     * 导致「最终状态」与「起止/受理时间」静默不落库（2026-11-20 实车：日志已收口
     * SUCCEEDED，库中仍为 PERMITTED 且四个时间列全空）。此语句显式写入关键列，
     * 时间列用 COALESCE 只补不覆盖，水位用 GREATEST 单调不减。</p>
     */
    @Update("UPDATE tb_task_vehicle_execution SET "
            + "status = #{status}, "
            + "start_time = COALESCE(start_time, #{startTime}), "
            + "start_received_time = COALESCE(start_received_time, #{startReceivedTime}), "
            + "end_time = COALESCE(end_time, #{endTime}), "
            + "end_received_time = COALESCE(end_received_time, #{endReceivedTime}), "
            + "final_sequence_no = CASE WHEN #{finalSequenceNo} > final_sequence_no "
            + "THEN #{finalSequenceNo} ELSE final_sequence_no END, "
            + "accepted_sequence_no = GREATEST(accepted_sequence_no, #{acceptedSequenceNo}), "
            + "modify_time = NOW(3), row_version = row_version + 1 "
            + "WHERE id = #{id} AND row_valid = 1")
    int updateFinalizeFields(@Param("id") Long id,
                             @Param("status") String status,
                             @Param("startTime") java.util.Date startTime,
                             @Param("startReceivedTime") java.util.Date startReceivedTime,
                             @Param("endTime") java.util.Date endTime,
                             @Param("endReceivedTime") java.util.Date endReceivedTime,
                             @Param("finalSequenceNo") Long finalSequenceNo,
                             @Param("acceptedSequenceNo") Long acceptedSequenceNo);

    /**
     * 按主键查询并对该行加悲观写锁，用于事件水位/收口的串行化。
     */
    @Select("SELECT * FROM tb_task_vehicle_execution WHERE id = #{id} AND row_valid = 1 FOR UPDATE")
    OtaExecutionPo selectByIdForUpdate(@Param("id") Long id);

    /**
     * 单调推进事件水位：只允许增大，禁止整行覆盖写把水位写回小值。
     *
     * <p>实车 2026-11-15：车端已 ACKED seq2，云端 accepted_sequence_no 回退到 1，
     * seq3.. 永久 BUFFERED（死锁）。
     */
    @Update("UPDATE tb_task_vehicle_execution SET accepted_sequence_no = GREATEST(accepted_sequence_no, #{acceptedSequenceNo}) "
            + "WHERE id = #{id} AND row_valid = 1")
    int updateAcceptedSequenceNoMonotonic(@Param("id") Long id,
                                          @Param("acceptedSequenceNo") Long acceptedSequenceNo);
}
