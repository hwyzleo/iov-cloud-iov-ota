package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlAckPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安装执行控制回执 DAO（CR-012）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExecutionControlAckMapper extends BaseMapper<ExecutionControlAckPo> {
}
