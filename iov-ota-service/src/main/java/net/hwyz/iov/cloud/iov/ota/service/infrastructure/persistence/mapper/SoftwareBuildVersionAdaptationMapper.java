package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionAdaptationPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 软件内部版本软硬件适配矩阵 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionAdaptationMapper extends BaseDao<SoftwareBuildVersionAdaptationPo, Long> {

    /**
     * 根据软件内部版本ID查询适配矩阵
     *
     * @param sbvId 软件内部版本ID
     * @return 适配矩阵列表
     */
    List<SoftwareBuildVersionAdaptationPo> selectPoBySbvId(Long sbvId);

    /**
     * 根据软件内部版本ID删除适配矩阵
     *
     * @param sbvId 软件内部版本ID
     * @return 删除数量
     */
    int deletePoBySbvId(Long sbvId);

    /**
     * 根据软件内部版本ID统计适配矩阵数量
     *
     * @param sbvId 软件内部版本ID
     * @return 数量
     */
    int countBySbvId(Long sbvId);

}
