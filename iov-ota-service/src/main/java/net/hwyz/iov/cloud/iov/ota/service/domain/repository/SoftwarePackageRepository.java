package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SoftwarePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;

import java.util.List;
import java.util.Map;

/**
 * 软件包Repository接口
 */
public interface SoftwarePackageRepository {
    
    SoftwarePackage findById(SoftwarePackageId id);
    
    List<SoftwarePackage> findByIds(List<SoftwarePackageId> ids);
    
    List<SoftwarePackage> search(Map<String, Object> params);
    
    void save(SoftwarePackage softwarePackage);
    
    void deleteByIds(List<SoftwarePackageId> ids);
}
