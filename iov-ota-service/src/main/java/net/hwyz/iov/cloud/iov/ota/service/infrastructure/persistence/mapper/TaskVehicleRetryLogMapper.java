package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleRetryLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 重试/续传轨迹审计 Mapper 接口
 */
@Mapper
public interface TaskVehicleRetryLogMapper extends BaseMapper<TaskVehicleRetryLogPo> {
    
}
