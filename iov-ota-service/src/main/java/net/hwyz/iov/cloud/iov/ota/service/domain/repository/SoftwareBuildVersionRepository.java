package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.util.List;
import java.util.Map;

/**
 * 软件内部版本Repository接口
 */
public interface SoftwareBuildVersionRepository {
    
    SoftwareBuildVersion findById(SoftwareBuildVersionId id);
    
    SoftwareBuildVersion findByDeviceCodeAndPnAndVersion(DeviceCode deviceCode, SoftwarePn pn, String version);
    
    List<SoftwareBuildVersion> search(Map<String, Object> params);
    
    List<SoftwareBuildVersion> listByBaselineCode(String baselineCode);
    
    void save(SoftwareBuildVersion version);
    
    void deleteByIds(List<SoftwareBuildVersionId> ids);
    
    int countPackages(SoftwareBuildVersionId id);
    
    int countDependencies(SoftwareBuildVersionId id);
}
