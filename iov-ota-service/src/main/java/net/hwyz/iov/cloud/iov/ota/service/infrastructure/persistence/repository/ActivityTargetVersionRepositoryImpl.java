package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityTargetVersion;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityTargetVersionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityTargetVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ActivityTargetVersion Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityTargetVersionRepositoryImpl implements ActivityTargetVersionRepository {

    private final ActivityTargetVersionMapper mapper;

    @Override
    public Optional<ActivityTargetVersion> getById(Long id) {
        ActivityTargetVersionPo po = mapper.selectPoById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public ActivityTargetVersion save(ActivityTargetVersion entity) {
        ActivityTargetVersionPo po = toPo(entity);
        if (entity.getId() == null) {
            mapper.insertPo(po);
            entity.setId(po.getId());
        } else {
            mapper.updatePo(po);
        }
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        mapper.logicalDeletePo(id);
    }

    private ActivityTargetVersion toDomain(ActivityTargetVersionPo po) {
        ActivityTargetVersion domain = new ActivityTargetVersion();
        domain.setId(po.getId());
        domain.setActivityId(po.getActivityId());
        domain.setBaselineCode(po.getBaselineCode());
        domain.setVehicleNodeCode(po.getVehicleNodeCode());
        domain.setPartCode(po.getPartCode());
        domain.setTargetSoftwareBuildVer(po.getTargetSoftwareBuildVer());
        return domain;
    }

    private ActivityTargetVersionPo toPo(ActivityTargetVersion domain) {
        ActivityTargetVersionPo po = new ActivityTargetVersionPo();
        po.setId(domain.getId());
        po.setActivityId(domain.getActivityId());
        po.setBaselineCode(domain.getBaselineCode());
        po.setVehicleNodeCode(domain.getVehicleNodeCode());
        po.setPartCode(domain.getPartCode());
        po.setTargetSoftwareBuildVer(domain.getTargetSoftwareBuildVer());
        return po;
    }
}
