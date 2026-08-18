package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PhaseGatePolicyPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阶段门禁阈值策略 Mapper 接口
 *
 * @author hwyz_leo
 */
@Mapper
public interface PhaseGatePolicyMapper extends BaseMapper<PhaseGatePolicyPo> {

}
