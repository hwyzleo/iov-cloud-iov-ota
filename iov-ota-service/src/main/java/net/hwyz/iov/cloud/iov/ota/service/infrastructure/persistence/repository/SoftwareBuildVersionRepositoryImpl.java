package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter.SoftwareBuildVersionConverter;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionDependencyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 软件内部版本Repository实现
 */
@Repository
@RequiredArgsConstructor
public class SoftwareBuildVersionRepositoryImpl implements SoftwareBuildVersionRepository {
    
    private final SoftwareBuildVersionMapper mapper;
    private final SoftwareBuildVersionConverter converter;
    private final SoftwareBuildVersionPackageMapper packageMapper;
    private final SoftwareBuildVersionDependencyMapper dependencyMapper;
    
    @Override
    public SoftwareBuildVersion findById(SoftwareBuildVersionId id) {
        SoftwareBuildVersionPo po = mapper.selectPoById(id.getValue());
        return converter.toDomain(po);
    }
    
    @Override
    public SoftwareBuildVersion findByDeviceCodeAndPnAndVersion(DeviceCode deviceCode, SoftwarePn pn, String version) {
        SoftwareBuildVersionPo po = mapper.selectPoByDeviceCodeAndSoftwarePnAndSoftwareBuildVer(
            deviceCode.getValue(), pn.getValue(), version
        );
        return converter.toDomain(po);
    }
    
    @Override
    public List<SoftwareBuildVersion> search(Map<String, Object> params) {
        List<SoftwareBuildVersionPo> poList = mapper.selectPoByMap(params);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<SoftwareBuildVersion> listByBaselineCode(String baselineCode) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("baselineCode", baselineCode);
        return search(params);
    }
    
    @Override
    public void save(SoftwareBuildVersion version) {
        SoftwareBuildVersionPo po = converter.toPo(version);
        if (version.getId() == null || version.getId().getValue() == null) {
            mapper.insertPo(po);
        } else {
            mapper.updatePo(po);
        }
    }
    
    @Override
    public void deleteByIds(List<SoftwareBuildVersionId> ids) {
        Long[] idArray = ids.stream()
            .map(SoftwareBuildVersionId::getValue)
            .toArray(Long[]::new);
        mapper.batchPhysicalDeletePo(idArray);
    }
    
    @Override
    public int countPackages(SoftwareBuildVersionId id) {
        return packageMapper.countBySoftwareBuildVersionId(id.getValue());
    }
    
    @Override
    public int countDependencies(SoftwareBuildVersionId id) {
        return dependencyMapper.countBySoftwareBuildVersionId(id.getValue());
    }
}
