package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaBaseException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.SoftwareBuildVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwareBuildVersionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SoftwareBuildVersionRepository;

/**
 * 软件内部版本领域服务
 * 处理跨聚合的业务规则
 */
@RequiredArgsConstructor
public class SoftwareBuildVersionDomainService {
    
    private final SoftwareBuildVersionRepository repository;

    /**
     * 检查唯一性（跨聚合的业务规则）
     */
    public boolean checkUnique(DeviceCode deviceCode, SoftwarePn softwarePn, 
                               String softwareBuildVer, SoftwareBuildVersionId excludeId) {
        SoftwareBuildVersion existing = repository.findByDeviceCodeAndPnAndVersion(
            deviceCode, softwarePn, softwareBuildVer
        );
        if (existing == null) return true;
        return existing.getId().equals(excludeId);
    }

    /**
     * 检查唯一性并抛出异常
     */
    public void validateUnique(DeviceCode deviceCode, SoftwarePn softwarePn, 
                               String softwareBuildVer, SoftwareBuildVersionId excludeId) {
        if (!checkUnique(deviceCode, softwarePn, softwareBuildVer, excludeId)) {
            throw new OtaBaseException("软件版本已存在：设备代码=" + deviceCode.getValue()
                + ", 软件零件号=" + softwarePn.getValue() + ", 版本=" + softwareBuildVer);
        }
    }
}
