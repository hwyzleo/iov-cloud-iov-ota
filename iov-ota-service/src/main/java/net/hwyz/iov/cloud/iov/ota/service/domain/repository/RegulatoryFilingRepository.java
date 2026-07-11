package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.RegulatoryFiling;

import java.util.Optional;

/**
 * RegulatoryFiling 仓储接口
 *
 * @author hwyz_leo
 */
public interface RegulatoryFilingRepository {

    Optional<RegulatoryFiling> getById(Long id);

    RegulatoryFiling save(RegulatoryFiling entity);

    void deleteById(Long id);
}
