package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.CompatiblePn;

import java.util.List;
import java.util.Map;

/**
 * 兼件号Repository接口
 */
public interface CompatiblePnRepository {
    
    CompatiblePn findById(Long id);
    
    List<CompatiblePn> search(Map<String, Object> params);
    
    void save(CompatiblePn compatiblePn);
    
    void deleteByIds(List<Long> ids);
}
