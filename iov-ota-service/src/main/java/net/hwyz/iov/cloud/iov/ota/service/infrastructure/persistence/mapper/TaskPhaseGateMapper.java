package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskPhaseGatePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跨任务阶段门禁 Mapper 接口
 */
@Mapper
public interface TaskPhaseGateMapper extends BaseMapper<TaskPhaseGatePo> {
    
}