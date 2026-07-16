package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.InstallConditionType;

import java.util.List;
import java.util.Optional;

/**
 * 安装条件类型仓储接口
 */
public interface InstallConditionTypeRepository {
    
    Optional<InstallConditionType> getById(Long id);
    
    Optional<InstallConditionType> getByCode(String code);
    
    List<InstallConditionType> listAll();
    
    List<InstallConditionType> listByPhase(String phase);
    
    List<InstallConditionType> listMandatory();
    
    InstallConditionType save(InstallConditionType entity);
    
    void deleteById(Long id);
}
