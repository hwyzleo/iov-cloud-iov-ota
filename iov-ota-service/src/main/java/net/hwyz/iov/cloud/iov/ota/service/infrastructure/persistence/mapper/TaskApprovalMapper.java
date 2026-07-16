package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskApprovalPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务审批 Mapper 接口
 */
@Mapper
public interface TaskApprovalMapper extends BaseMapper<TaskApprovalPo> {
    
}