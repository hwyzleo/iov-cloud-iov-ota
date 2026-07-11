package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserConsent DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface UserConsentMapper extends BaseMapper<UserConsentPo> {

}
