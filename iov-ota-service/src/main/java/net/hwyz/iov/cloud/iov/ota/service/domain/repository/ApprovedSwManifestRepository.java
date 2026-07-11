package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ApprovedSwManifest;

import java.util.Optional;

/**
 * ApprovedSwManifest 仓储接口
 *
 * @author hwyz_leo
 */
public interface ApprovedSwManifestRepository {

    Optional<ApprovedSwManifest> getById(Long id);

    ApprovedSwManifest save(ApprovedSwManifest entity);

    void deleteById(Long id);
}
