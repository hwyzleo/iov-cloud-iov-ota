package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradePackageBuild;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradePackageBuildRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UpgradePackageBuildMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradePackageBuildPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UpgradePackageBuild Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UpgradePackageBuildRepositoryImpl implements UpgradePackageBuildRepository {

    private final UpgradePackageBuildMapper mapper;

    @Override
    public Optional<UpgradePackageBuild> getById(Long id) {
        UpgradePackageBuildPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public UpgradePackageBuild save(UpgradePackageBuild entity) {
        UpgradePackageBuildPo po = toPo(entity);
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

    private UpgradePackageBuild toDomain(UpgradePackageBuildPo po) {
        UpgradePackageBuild domain = new UpgradePackageBuild();
        domain.setId(po.getId());
        return domain;
    }

    private UpgradePackageBuildPo toPo(UpgradePackageBuild domain) {
        UpgradePackageBuildPo po = new UpgradePackageBuildPo();
        po.setId(domain.getId());
        return po;
    }
}
