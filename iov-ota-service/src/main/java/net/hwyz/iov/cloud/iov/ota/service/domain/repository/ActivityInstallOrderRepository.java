package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityInstallOrder;

import java.util.Optional;

/**
 * ActivityInstallOrder 仓储接口
 *
 * @author hwyz_leo
 */
public interface ActivityInstallOrderRepository {

    Optional<ActivityInstallOrder> getById(Long id);

    ActivityInstallOrder save(ActivityInstallOrder entity);

    void deleteById(Long id);
}
