package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradePackageBuild;

import java.util.Optional;

/**
 * UpgradePackageBuild 仓储接口
 *
 * @author hwyz_leo
 */
public interface UpgradePackageBuildRepository {

    Optional<UpgradePackageBuild> getById(Long id);

    UpgradePackageBuild save(UpgradePackageBuild entity);

    void deleteById(Long id);
}
