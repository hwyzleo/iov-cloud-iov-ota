package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.VehiclePartConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehiclePartPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆零件Repository实现
 */
@Repository
@RequiredArgsConstructor
public class VehiclePartRepositoryImpl implements VehiclePartRepository {
    
    private final VehiclePartMapper mapper;
    private final VehiclePartConverter converter;
    
    @Override
    public VehiclePart findById(Long id) {
        VehiclePartPo po = mapper.selectPoById(id);
        return converter.toDomain(po);
    }
    
    @Override
    public List<VehiclePart> search(Map<String, Object> params) {
        List<VehiclePartPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(VehiclePart vehiclePart) {
        VehiclePartPo po = converter.toPo(vehiclePart);
        if (vehiclePart.getId() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<Long> ids) {
        Long[] idArray = ids.toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
}
