package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityTargetVersion;

import java.util.Optional;

/**
 * ActivityTargetVersion 仓储接口
 *
 * @author hwyz_leo
 */
public interface ActivityTargetVersionRepository {

    Optional<ActivityTargetVersion> getById(Long id);

    ActivityTargetVersion save(ActivityTargetVersion entity);

    void deleteById(Long id);
}
