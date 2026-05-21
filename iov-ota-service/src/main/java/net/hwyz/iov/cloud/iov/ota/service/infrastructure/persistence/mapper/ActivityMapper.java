package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 升级活动表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-09-12
 */
@Mapper
public interface ActivityMapper extends BaseDao<ActivityPo, Long> {

}
