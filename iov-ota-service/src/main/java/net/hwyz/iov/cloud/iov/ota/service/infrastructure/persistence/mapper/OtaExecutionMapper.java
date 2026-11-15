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
