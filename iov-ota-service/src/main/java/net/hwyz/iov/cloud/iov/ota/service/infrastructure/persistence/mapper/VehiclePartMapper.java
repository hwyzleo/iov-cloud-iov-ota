package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
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
public interface VehiclePartMapper extends BaseDao<VehiclePartPo, Long> {

    /**
     * 根据车辆vin和零件pn查询车辆零件信息
     *
     * @param vin 车辆vin
     * @param pn  零件pn
     * @return 车辆零件信息
     */
    VehiclePartPo selectPoByVinAndPn(String vin, String pn);
    /**
     * 按 VIN 批量查询有效零件投影（CR-023 §5：一次查询，按 deviceCode 内存分组）
     *
     * @param vin 车架号
     * @return 有效零件列表
     */
    java.util.List<VehiclePartPo> selectByVin(@org.apache.ibatis.annotations.Param("vin") String vin);

    /**
     * 全受管字段更新（CR-023 §6.1）
     *
     * <p>与通用 updatePo 不同：受管字段无条件赋值，允许将字段清空为 NULL/空串，
     * 保证 VMD 同步能够完整反映当前投影事实。vin + pn 身份键不参与更新。
     *
     * @param vehiclePartPo 零件投影（id 必须存在）
     * @return 影响行数
     */
    int updateManagedFields(VehiclePartPo vehiclePartPo);

}
