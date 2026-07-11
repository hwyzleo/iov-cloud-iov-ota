package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * ActivityInstallOrder DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface ActivityInstallOrderMapper extends BaseMapper<ActivityInstallOrderPo> {

}
