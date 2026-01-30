package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionDependencyPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 软件内部版本依赖关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Mapper
public interface SoftwareBuildVersionDependencyDao extends BaseDao<SoftwareBuildVersionDependencyPo, Long> {

    /**
     * 根据软件内部版本ID查询
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件内部版本依赖关系列表
     */
    List<SoftwareBuildVersionDependencyPo> selectPoBySoftwareBuildVersionId(Long softwareBuildVersionId);

    /**
     * 批量物理删除指定软件内部版本下的依赖关系
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 依赖的软件内部版本ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoBySoftwareBuildVersionIdAndDependencyIds(Long softwareBuildVersionId, Long[] softwareBuildVersionIds);

    /**
     * 根据软件内部版本统计数量
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 数量
     */
    int countBySoftwareBuildVersionId(Long softwareBuildVersionId);

}
