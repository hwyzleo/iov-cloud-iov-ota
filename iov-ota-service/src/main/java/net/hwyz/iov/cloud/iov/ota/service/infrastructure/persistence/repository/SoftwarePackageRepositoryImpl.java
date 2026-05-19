package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwarePackageRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.SoftwarePackageConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 软件包Repository实现
 */
@Repository
@RequiredArgsConstructor
public class SoftwarePackageRepositoryImpl implements SoftwarePackageRepository {
    
    private final SoftwarePackageMapper mapper;
    private final SoftwarePackageConverter converter;
    
    @Override
    public SoftwarePackage findById(SoftwarePackageId id) {
        SoftwarePackagePo po = mapper.selectPoById(id.getValue());
        return converter.toDomain(po);
    }
    
    @Override
    public List<SoftwarePackage> findByIds(List<SoftwarePackageId> ids) {
        List<Long> idValues = ids.stream()
            .map(SoftwarePackageId::getValue)
            .toList();
        // 使用selectPoByMap批量查询
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("ids", idValues);
        List<SoftwarePackagePo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<SoftwarePackage> search(Map<String, Object> params) {
        List<SoftwarePackagePo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public void save(SoftwarePackage pkg) {
        SoftwarePackagePo po = converter.toPo(pkg);
        if (pkg.getId() == null || pkg.getId().getValue() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<SoftwarePackageId> ids) {
        Long[] idArray = ids.stream()
            .map(SoftwarePackageId::getValue)
            .toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
}
