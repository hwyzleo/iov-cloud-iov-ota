package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.Baseline;

import java.util.Optional;

/**
 * Baseline 仓储接口
 *
 * @author hwyz_leo
 */
public interface BaselineRepository {

    Optional<Baseline> getById(Long id);

    Optional<Baseline> getByBaselineCode(String baselineCode);

    Baseline save(Baseline entity);

    void deleteById(Long id);
}
