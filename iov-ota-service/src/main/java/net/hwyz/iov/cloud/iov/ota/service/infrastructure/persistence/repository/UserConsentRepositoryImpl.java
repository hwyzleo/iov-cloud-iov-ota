package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UserConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UserConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UserConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserConsent Repository实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserConsentRepositoryImpl implements UserConsentRepository {

    private final UserConsentMapper mapper;

    @Override
    public Optional<UserConsent> getById(Long id) {
        UserConsentPo po = mapper.selectById(id);
        return Optional.ofNullable(po != null && Boolean.TRUE.equals(po.getRowValid()) ? toDomain(po) : null);
    }

    @Override
    public UserConsent save(UserConsent entity) {
        UserConsentPo po = toPo(entity);
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

    private UserConsent toDomain(UserConsentPo po) {
        UserConsent domain = new UserConsent();
        domain.setId(po.getId());
        return domain;
    }

    private UserConsentPo toPo(UserConsent domain) {
        UserConsentPo po = new UserConsentPo();
        po.setId(domain.getId());
        return po;
    }
}
