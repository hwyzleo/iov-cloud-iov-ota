package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.RegulatoryFiling;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.RegulatoryFilingRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.RegulatoryFilingMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RegulatoryFiling Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RegulatoryFilingRepositoryImpl implements RegulatoryFilingRepository {

    private final RegulatoryFilingMapper mapper;

    @Override
    public Optional<RegulatoryFiling> getById(Long id) {
        RegulatoryFilingPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public RegulatoryFiling save(RegulatoryFiling entity) {
        RegulatoryFilingPo po = toPo(entity);
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

    private RegulatoryFiling toDomain(RegulatoryFilingPo po) {
        RegulatoryFiling domain = new RegulatoryFiling();
        domain.setId(po.getId());
        return domain;
    }

    private RegulatoryFilingPo toPo(RegulatoryFiling domain) {
        RegulatoryFilingPo po = new RegulatoryFilingPo();
        po.setId(domain.getId());
        return po;
    }
}
