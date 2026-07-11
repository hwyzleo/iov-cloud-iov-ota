package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradePackagePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * UpgradePackage DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface UpgradePackageMapper extends BaseMapper<UpgradePackagePo> {

}
