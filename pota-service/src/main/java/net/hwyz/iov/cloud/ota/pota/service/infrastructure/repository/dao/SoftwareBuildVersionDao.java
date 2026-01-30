package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 软件内部版本信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-30
 */
@Mapper
public interface SoftwareBuildVersionDao extends BaseDao<SoftwareBuildVersionPo, Long> {

    /**
     * 根据设备代码及软件零件号及版本及内部版本查询软件内部版本信息
     *
     * @param deviceCode       设备代码
     * @param softwarePn       软件零件号
     * @param softwareBuildVer 软件内部版本
     * @return 软件内部版本信息
     */
    SoftwareBuildVersionPo selectPoByDeviceCodeAndSoftwarePnAndSoftwareBuildVer(String deviceCode, String softwarePn,
                                                                                String softwareBuildVer);

}
