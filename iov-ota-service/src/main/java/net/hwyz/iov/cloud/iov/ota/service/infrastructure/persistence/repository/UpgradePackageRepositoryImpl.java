package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradePackage;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradePackageRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UpgradePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradePackagePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UpgradePackage Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UpgradePackageRepositoryImpl implements UpgradePackageRepository {

    private final UpgradePackageMapper mapper;

    @Override
    public Optional<UpgradePackage> getById(Long id) {
        UpgradePackagePo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public UpgradePackage save(UpgradePackage entity) {
        UpgradePackagePo po = toPo(entity);
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

    private UpgradePackage toDomain(UpgradePackagePo po) {
        UpgradePackage domain = new UpgradePackage();
        domain.setId(po.getId());
        return domain;
    }

    private UpgradePackagePo toPo(UpgradePackage domain) {
        UpgradePackagePo po = new UpgradePackagePo();
        po.setId(domain.getId());
        return po;
    }
}
