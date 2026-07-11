package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinDefinition;

import java.util.Optional;

/**
 * SwinDefinition 仓储接口
 *
 * @author hwyz_leo
 */
public interface SwinDefinitionRepository {

    Optional<SwinDefinition> getById(Long id);

    SwinDefinition save(SwinDefinition entity);

    void deleteById(Long id);
}
