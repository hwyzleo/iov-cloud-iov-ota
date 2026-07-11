package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDependencyGroup;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityDependencyGroupRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityDependencyGroupMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityDependencyGroupPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ActivityDependencyGroup Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityDependencyGroupRepositoryImpl implements ActivityDependencyGroupRepository {

    private final ActivityDependencyGroupMapper mapper;

    @Override
    public Optional<ActivityDependencyGroup> getById(Long id) {
        ActivityDependencyGroupPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public ActivityDependencyGroup save(ActivityDependencyGroup entity) {
        ActivityDependencyGroupPo po = toPo(entity);
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

    private ActivityDependencyGroup toDomain(ActivityDependencyGroupPo po) {
        ActivityDependencyGroup domain = new ActivityDependencyGroup();
        domain.setId(po.getId());
        return domain;
    }

    private ActivityDependencyGroupPo toPo(ActivityDependencyGroup domain) {
        ActivityDependencyGroupPo po = new ActivityDependencyGroupPo();
        po.setId(domain.getId());
        return po;
    }
}
