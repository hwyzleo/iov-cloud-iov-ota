package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradePackage;

import java.util.Optional;

/**
 * UpgradePackage 仓储接口
 *
 * @author hwyz_leo
 */
public interface UpgradePackageRepository {

    Optional<UpgradePackage> getById(Long id);

    UpgradePackage save(UpgradePackage entity);

    void deleteById(Long id);
}
