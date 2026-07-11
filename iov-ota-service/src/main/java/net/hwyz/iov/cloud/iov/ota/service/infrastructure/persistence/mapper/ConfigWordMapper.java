package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 配置字表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-11
 */
@Mapper
public interface ConfigWordMapper extends BaseDao<ConfigWordPo, Long> {

    /**
     * 根据配置字代码获取配置字
     *
     * @param code 配置字代码
     * @return 配置字
     */
    ConfigWordPo selectPoByCode(String code);

}
