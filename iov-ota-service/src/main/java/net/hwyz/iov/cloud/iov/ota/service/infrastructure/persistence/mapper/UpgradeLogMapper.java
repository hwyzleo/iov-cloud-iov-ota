package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradeLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * UpgradeLog DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface UpgradeLogMapper extends BaseMapper<UpgradeLogPo> {

}
