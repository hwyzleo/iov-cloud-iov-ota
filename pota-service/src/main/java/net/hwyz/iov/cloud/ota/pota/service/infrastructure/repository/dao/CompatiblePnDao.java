package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.CompatiblePnPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 兼容零件号表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Mapper
public interface CompatiblePnDao extends BaseDao<CompatiblePnPo, Long> {

}
