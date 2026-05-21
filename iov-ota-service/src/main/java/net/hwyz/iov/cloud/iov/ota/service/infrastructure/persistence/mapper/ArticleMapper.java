package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ArticlePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文章表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-09-15
 */
@Mapper
public interface ArticleMapper extends BaseDao<ArticlePo, Long> {

}
