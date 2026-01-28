package net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.VehiclePartPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆零件表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-28
 */
@Mapper
public interface VehiclePartDao extends BaseDao<VehiclePartPo, Long> {

    /**
     * 根据车辆vin和零件pn查询车辆零件信息
     *
     * @param vin 车辆vin
     * @param pn  零件pn
     * @return 车辆零件信息
     */
    VehiclePartPo selectPoByVinAndPn(String vin, String pn);

}
