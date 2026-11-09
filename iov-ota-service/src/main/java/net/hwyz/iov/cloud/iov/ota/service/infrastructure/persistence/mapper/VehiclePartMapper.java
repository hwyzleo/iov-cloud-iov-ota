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
     * 根据车辆vin和零件pn查询全部有效零件投影（vin + pn 业务身份键）
     *
     * <p>与 {@link #selectPoByVinAndPn(String, String)} 的区别：不做 LIMIT 1，
     * 以便 UNBIND 场景删除历史遗留重复行。绝不可用 {@code selectPoByMap} 代替，
     * 该通用语句在全表范围内返回数据，会把其他 VIN 的行当成命中行改写/删除。</p>
     *
     * @param vin 车架号
     * @param pn  零件编号
     * @return 命中行列表（按 id 倒序，通常 0 或 1 条）
     */
    java.util.List<VehiclePartPo> selectPoListByVinAndPn(@org.apache.ibatis.annotations.Param("vin") String vin,
                                                         @org.apache.ibatis.annotations.Param("pn") String pn);
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
