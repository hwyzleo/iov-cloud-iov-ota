package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;

import java.util.Optional;

/**
 * UpgradeLog 仓储接口
 *
 * @author hwyz_leo
 */
public interface UpgradeLogRepository {

    Optional<UpgradeLog> getById(Long id);

    UpgradeLog save(UpgradeLog entity);

    void deleteById(Long id);
}
