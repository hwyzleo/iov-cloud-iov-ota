package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinManagedSystem;

import java.util.Optional;

/**
 * SwinManagedSystem 仓储接口
 *
 * @author hwyz_leo
 */
public interface SwinManagedSystemRepository {

    Optional<SwinManagedSystem> getById(Long id);

    SwinManagedSystem save(SwinManagedSystem entity);

    void deleteById(Long id);
}
