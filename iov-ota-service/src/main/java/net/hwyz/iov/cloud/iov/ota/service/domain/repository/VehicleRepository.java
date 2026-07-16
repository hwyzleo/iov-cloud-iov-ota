package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;

/**
 * 车辆领域仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleRepository extends BaseRepository<String, VehicleDo> {
    
    /**
     * 根据条件查询车辆
     * @param field 字段名
     * @param operator 操作符
     * @param value 值
     * @return 车辆列表
     */
    java.util.List<VehicleDo> findByCondition(String field, String operator, String value);
    
    /**
     * 根据VIN列表批量查询车辆
     * @param vins VIN列表
     * @return 车辆列表
     */
    java.util.List<VehicleDo> findByVins(java.util.List<String> vins);
    
    /**
     * 查询所有车辆
     * @return 车辆列表
     */
    java.util.List<VehicleDo> findAll();
}
