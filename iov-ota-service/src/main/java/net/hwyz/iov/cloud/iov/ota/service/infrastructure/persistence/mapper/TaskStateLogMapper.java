package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStateLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务状态迁移审计 Mapper 接口
 */
@Mapper
public interface TaskStateLogMapper extends BaseMapper<TaskStateLogPo> {
    
}
