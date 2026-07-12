package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionTestReportPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 软件内部版本测试报告 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface SoftwareBuildVersionTestReportMapper extends BaseDao<SoftwareBuildVersionTestReportPo, Long> {

    /**
     * 根据软件内部版本ID查询测试报告
     *
     * @param sbvId 软件内部版本ID
     * @return 测试报告列表
     */
    List<SoftwareBuildVersionTestReportPo> selectPoBySbvId(Long sbvId);

    /**
     * 根据软件内部版本ID删除测试报告
     *
     * @param sbvId 软件内部版本ID
     * @return 删除数量
     */
    int deletePoBySbvId(Long sbvId);

    /**
     * 根据软件内部版本ID统计测试报告数量
     *
     * @param sbvId 软件内部版本ID
     * @return 数量
     */
    int countBySbvId(Long sbvId);

}
