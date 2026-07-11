package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityInstallOrder;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityInstallOrderRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityInstallOrderMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ActivityInstallOrder Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityInstallOrderRepositoryImpl implements ActivityInstallOrderRepository {

    private final ActivityInstallOrderMapper mapper;

    @Override
    public Optional<ActivityInstallOrder> getById(Long id) {
        ActivityInstallOrderPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public ActivityInstallOrder save(ActivityInstallOrder entity) {
        ActivityInstallOrderPo po = toPo(entity);
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

    private ActivityInstallOrder toDomain(ActivityInstallOrderPo po) {
        ActivityInstallOrder domain = new ActivityInstallOrder();
        domain.setId(po.getId());
        return domain;
    }

    private ActivityInstallOrderPo toPo(ActivityInstallOrder domain) {
        ActivityInstallOrderPo po = new ActivityInstallOrderPo();
        po.setId(domain.getId());
        return po;
    }
}
