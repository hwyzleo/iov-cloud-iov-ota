package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradePackageBuildPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * UpgradePackageBuild DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface UpgradePackageBuildMapper extends BaseMapper<UpgradePackageBuildPo> {

}
