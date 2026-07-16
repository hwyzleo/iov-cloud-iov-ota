package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PhaseApprovalPolicyPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阶段审批策略 Mapper 接口
 */
@Mapper
public interface PhaseApprovalPolicyMapper extends BaseMapper<PhaseApprovalPolicyPo> {
    
}