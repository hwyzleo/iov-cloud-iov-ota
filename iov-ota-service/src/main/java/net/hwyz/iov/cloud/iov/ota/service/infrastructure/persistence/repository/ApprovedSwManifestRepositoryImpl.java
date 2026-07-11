package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ApprovedSwManifest;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ApprovedSwManifestRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ApprovedSwManifestMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ApprovedSwManifest Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ApprovedSwManifestRepositoryImpl implements ApprovedSwManifestRepository {

    private final ApprovedSwManifestMapper mapper;

    @Override
    public Optional<ApprovedSwManifest> getById(Long id) {
        ApprovedSwManifestPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public ApprovedSwManifest save(ApprovedSwManifest entity) {
        ApprovedSwManifestPo po = toPo(entity);
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

    private ApprovedSwManifest toDomain(ApprovedSwManifestPo po) {
        ApprovedSwManifest domain = new ApprovedSwManifest();
        domain.setId(po.getId());
        return domain;
    }

    private ApprovedSwManifestPo toPo(ApprovedSwManifest domain) {
        ApprovedSwManifestPo po = new ApprovedSwManifestPo();
        po.setId(domain.getId());
        return po;
    }
}
