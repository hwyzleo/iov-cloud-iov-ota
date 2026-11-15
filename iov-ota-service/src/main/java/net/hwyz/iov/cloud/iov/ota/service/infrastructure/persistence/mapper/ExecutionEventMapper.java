package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 安装执行事件 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionEventMapper extends BaseMapper<ExecutionEventPo> {

    @Select("SELECT * FROM tb_task_vehicle_execution_event WHERE execution_id = #{executionId} AND row_valid = 1 ORDER BY sequence_no")
    List<ExecutionEventPo> selectByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT * FROM tb_task_vehicle_execution_event WHERE event_id = #{eventId} AND row_valid = 1")
    ExecutionEventPo selectByEventId(@Param("eventId") String eventId);

    /**
     * 已收到（落库）的全部事件序号，升序。
     *
     * <p>水位与乱序缓冲都从事件事实重建：buffered 不落在聚合上（实车 2026-11-15：
     * 水位回退到 1 后，seq3..32 永久 BUFFERED 且 missing_sequence_ranges 恒为空）。
     * 只取序号列，避免加载 event_payload 大字段。
     */
    @Select("SELECT sequence_no FROM tb_task_vehicle_execution_event "
            + "WHERE execution_id = #{executionId} AND row_valid = 1 "
            + "AND disposition IN ('ACCEPTED', 'BUFFERED', 'DUPLICATE') ORDER BY sequence_no")
    List<Long> selectReceivedSequenceNos(@Param("executionId") Long executionId);

    /**
     * 水位推进后吸收已暂存的乱序事件：把 ≤ 水位的 BUFFERED 行置为 ACCEPTED。
     */
    @Update("UPDATE tb_task_vehicle_execution_event SET disposition = 'ACCEPTED' "
            + "WHERE execution_id = #{executionId} AND row_valid = 1 AND disposition = 'BUFFERED' "
            + "AND sequence_no <= #{acceptedSequenceNo}")
    int markBufferedAbsorbed(@Param("executionId") Long executionId,
                             @Param("acceptedSequenceNo") Long acceptedSequenceNo);
}
