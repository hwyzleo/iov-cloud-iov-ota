package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordFieldPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 配置字字段表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-11
 */
@Mapper
public interface ConfigWordFieldMapper extends BaseDao<ConfigWordFieldPo, Long> {

    /**
     * 根据字段代码查询字段
     *
     * @param configWordCode        配置字代码
     * @param configWordProfileCode 配置字配置文件代码
     * @param code                  字段代码
     * @return 字段
     */
    ConfigWordFieldPo selectPoByCode(String configWordCode, String configWordProfileCode, String code);

}
