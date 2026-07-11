package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinDefinition;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SwinDefinitionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinDefinitionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinDefinitionPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SwinDefinition Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SwinDefinitionRepositoryImpl implements SwinDefinitionRepository {

    private final SwinDefinitionMapper mapper;

    @Override
    public Optional<SwinDefinition> getById(Long id) {
        SwinDefinitionPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public SwinDefinition save(SwinDefinition entity) {
        SwinDefinitionPo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private SwinDefinition toDomain(SwinDefinitionPo po) {
        SwinDefinition domain = new SwinDefinition();
        domain.setId(po.getId());
        return domain;
    }

    private SwinDefinitionPo toPo(SwinDefinition domain) {
        SwinDefinitionPo po = new SwinDefinitionPo();
        po.setId(domain.getId());
        return po;
    }
}
