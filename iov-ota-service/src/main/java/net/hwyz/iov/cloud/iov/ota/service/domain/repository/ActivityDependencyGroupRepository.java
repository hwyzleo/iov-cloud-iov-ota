package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDependencyGroup;

import java.util.Optional;

/**
 * ActivityDependencyGroup 仓储接口
 *
 * @author hwyz_leo
 */
public interface ActivityDependencyGroupRepository {

    Optional<ActivityDependencyGroup> getById(Long id);

    ActivityDependencyGroup save(ActivityDependencyGroup entity);

    void deleteById(Long id);
}
