package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.SwinManagedSystem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.SwinManagedSystemRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinManagedSystemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinManagedSystemPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SwinManagedSystem Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SwinManagedSystemRepositoryImpl implements SwinManagedSystemRepository {

    private final SwinManagedSystemMapper mapper;

    @Override
    public Optional<SwinManagedSystem> getById(Long id) {
        SwinManagedSystemPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public SwinManagedSystem save(SwinManagedSystem entity) {
        SwinManagedSystemPo po = toPo(entity);
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

    private SwinManagedSystem toDomain(SwinManagedSystemPo po) {
        SwinManagedSystem domain = new SwinManagedSystem();
        domain.setId(po.getId());
        return domain;
    }

    private SwinManagedSystemPo toPo(SwinManagedSystem domain) {
        SwinManagedSystemPo po = new SwinManagedSystemPo();
        po.setId(domain.getId());
        return po;
    }
}
