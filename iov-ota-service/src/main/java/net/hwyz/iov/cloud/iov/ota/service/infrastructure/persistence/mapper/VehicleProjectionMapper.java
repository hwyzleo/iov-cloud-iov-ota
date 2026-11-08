package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Date;
import java.util.List;

/**
 * 车辆主档本地只读投影 DAO
 * <p>
 * CR-011: 消费 MDM/VMD VehicleProduceEvent
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VehicleProjectionMapper extends BaseDao<VehicleProjectionPo, Long> {

    /**
     * 根据车架号查询车辆投影
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    VehicleProjectionPo selectByVin(@Param("vin") String vin);

    /**
     * 根据车架号查询车辆投影（带行锁）
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    VehicleProjectionPo selectByVinForUpdate(@Param("vin") String vin);

    /**
     * 按上游版本更新投影（仅当 source_version < 新版本时更新）
     *
     * @param po 投影数据
     * @return 影响行数
     */
    int updateIfNewerVersion(VehicleProjectionPo po);

    /**
     * 根据条件查询车辆列表
     *
     * @param queryWrapper 查询条件
     * @return 车辆列表
     */
    java.util.List<VehicleProjectionPo> selectList(@Param("ew") QueryWrapper<VehicleProjectionPo> queryWrapper);

    /**
     * 升级车辆管理列表：主档投影一次 LEFT JOIN 运行态最后上报时间（CR-022 §5.1，禁止逐 VIN 查询）
     *
     * @param vin       车架号（模糊匹配，可空）
     * @param beginTime 创建时间起点（可空）
     * @param endTime   创建时间终点（可空）
     * @return 管理查询结果列表
     */
    List<VehicleManagementResult> selectManagementList(@Param("vin") String vin,
                                                       @Param("beginTime") Date beginTime,
                                                       @Param("endTime") Date endTime);

    /**
     * 升级车辆管理详情：主档投影 LEFT JOIN 运行态最后上报时间（CR-022 §5.1）
     *
     * @param vin 车架号
     * @return 管理查询结果，主档不存在返回 null
     */
    VehicleManagementResult selectManagementByVin(@Param("vin") String vin);

    /**
     * 车辆是否存在于主档投影（CR-023 §9：车辆存在性使用 tb_vehicle_projection，
     * 不使用 tb_veh_status）
     *
     * @param vin 车架号
     * @return 匹配行数（0/1）
     */
    int countByVin(@Param("vin") String vin);
}
