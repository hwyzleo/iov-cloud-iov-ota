package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.CompatiblePnRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.CompatiblePnConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.CompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 兼件号Repository实现
 */
@Repository
@RequiredArgsConstructor
public class CompatiblePnRepositoryImpl implements CompatiblePnRepository {
    
    private final CompatiblePnMapper mapper;
    private final CompatiblePnConverter converter;
    
    @Override
    public CompatiblePn findById(Long id) {
        CompatiblePnPo po = mapper.selectPoById(id);
        return converter.toDomain(po);
    }
    
    @Override
    public List<CompatiblePn> search(Map<String, Object> params) {
        List<CompatiblePnPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(CompatiblePn compatiblePn) {
        CompatiblePnPo po = converter.toPo(compatiblePn);
        if (compatiblePn.getId() == null) {
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
