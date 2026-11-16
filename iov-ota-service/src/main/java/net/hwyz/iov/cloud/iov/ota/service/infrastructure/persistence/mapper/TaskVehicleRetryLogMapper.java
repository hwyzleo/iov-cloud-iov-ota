package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 重试/续传轨迹审计 Mapper 接口
 */
@Mapper
public interface TaskVehicleRetryLogMapper extends BaseMapper<TaskVehicleRetryLogPo> {

    /**
     * 按业务幂等键读取不可变重试事实（CR-026 §7.4）。
     *
     * @param retryEventId 重试事件ID
     * @return 已存在的重试轨迹行
     */
    @Select("SELECT * FROM tb_task_vehicle_retry_log WHERE retry_event_id = #{retryEventId} LIMIT 1")
    TaskVehicleRetryLogPo selectByRetryEventId(@Param("retryEventId") String retryEventId);

}
