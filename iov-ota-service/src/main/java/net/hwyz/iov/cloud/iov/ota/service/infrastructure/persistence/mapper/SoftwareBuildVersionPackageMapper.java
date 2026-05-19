package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPackagePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 软件内部版本软件包关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Mapper
public interface SoftwareBuildVersionPackageMapper extends BaseDao<SoftwareBuildVersionPackagePo, Long> {

    /**
     * 根据软件内部版本ID查询
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 软件包列表
     */
    List<SoftwareBuildVersionPackagePo> selectPoBySoftwareBuildVersionId(Long softwareBuildVersionId);

    /**
     * 批量物理删除指定软件内部版本下的软件包关系
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID列表
     * @return 删除数量
     */
    int batchPhysicalDeletePoBySoftwareBuildVersionIdAndSoftwarePackageIds(Long softwareBuildVersionId, Long[] softwarePackageIds);

    /**
     * 根据软件内部版本统计数量
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @return 数量
     */
    int countBySoftwareBuildVersionId(Long softwareBuildVersionId);

}
