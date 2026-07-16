package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehStatusPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆状态表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-12-16
 */
@Mapper
public interface VehStatusMapper extends BaseDao<VehStatusPo, Long> {

    /**
     * 根据车架号查询车辆设置
     *
     * @param vin 车架号
     * @return 车辆设置
     */
    VehStatusPo selectByVin(String vin);

    /**
     * 根据条件查询车辆列表
     *
     * @param queryWrapper 查询条件
     * @return 车辆列表
     */
    java.util.List<VehStatusPo> selectList(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VehStatusPo> queryWrapper);

}
