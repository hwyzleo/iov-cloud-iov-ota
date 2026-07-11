package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradeLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UpgradeLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UpgradeLogPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UpgradeLog Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UpgradeLogRepositoryImpl implements UpgradeLogRepository {

    private final UpgradeLogMapper mapper;

    @Override
    public Optional<UpgradeLog> getById(Long id) {
        UpgradeLogPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public UpgradeLog save(UpgradeLog entity) {
        UpgradeLogPo po = toPo(entity);
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

    private UpgradeLog toDomain(UpgradeLogPo po) {
        UpgradeLog domain = new UpgradeLog();
        domain.setId(po.getId());
        return domain;
    }

    private UpgradeLogPo toPo(UpgradeLog domain) {
        UpgradeLogPo po = new UpgradeLogPo();
        po.setId(domain.getId());
        return po;
    }
}
