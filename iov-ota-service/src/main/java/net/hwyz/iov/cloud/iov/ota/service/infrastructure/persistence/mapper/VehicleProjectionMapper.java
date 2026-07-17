package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

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
}
