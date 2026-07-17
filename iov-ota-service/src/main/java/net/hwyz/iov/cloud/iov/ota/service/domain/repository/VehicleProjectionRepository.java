package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;

import java.util.List;
import java.util.Optional;

/**
 * 车辆主档本地只读投影仓储接口
 * <p>
 * CR-011: 消费 MDM/VMD VehicleProduceEvent
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
public interface VehicleProjectionRepository {

    /**
     * 根据车架号查询车辆投影
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    Optional<VehicleProjectionPo> findByVin(String vin);

    /**
     * 根据车架号查询车辆投影（带行锁）
     *
     * @param vin 车架号
     * @return 车辆投影
     */
    Optional<VehicleProjectionPo> findByVinForUpdate(String vin);

    /**
     * 保存车辆投影（插入）
     *
     * @param po 车辆投影
     */
    void insert(VehicleProjectionPo po);

    /**
     * 更新车辆投影
     *
     * @param po 车辆投影
     */
    void update(VehicleProjectionPo po);

    /**
     * 按上游版本更新投影（仅当 source_version < 新版本时更新）
     *
     * @param po 车辆投影
     * @return 是否更新成功
     */
    boolean updateIfNewerVersion(VehicleProjectionPo po);

    /**
     * 查询所有车辆投影
     *
     * @return 车辆投影列表
     */
    List<VehicleProjectionPo> findAll();

    /**
     * 根据配置编码查询车辆投影
     *
     * @param configurationCode 配置编码
     * @return 车辆投影列表
     */
    List<VehicleProjectionPo> findByConfigurationCode(String configurationCode);

    /**
     * 根据VIN列表批量查询车辆投影
     *
     * @param vins VIN列表
     * @return 车辆投影列表
     */
    List<VehicleProjectionPo> findByVins(List<String> vins);
}
