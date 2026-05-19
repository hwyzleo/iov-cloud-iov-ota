package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件Repository接口
 */
public interface VehiclePartRepository {
    
    VehiclePart findById(Long id);
    
    List<VehiclePart> search(Map<String, Object> params);
    
    void save(VehiclePart vehiclePart);
    
    void deleteByIds(List<Long> ids);
}
