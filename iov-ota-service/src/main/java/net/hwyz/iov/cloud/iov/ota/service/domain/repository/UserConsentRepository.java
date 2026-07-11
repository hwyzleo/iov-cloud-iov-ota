package net.hwyz.iov.cloud.iov.ota.service.domain.repository;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UserConsent;

import java.util.Optional;

/**
 * UserConsent 仓储接口
 *
 * @author hwyz_leo
 */
public interface UserConsentRepository {

    Optional<UserConsent> getById(Long id);

    UserConsent save(UserConsent entity);

    void deleteById(Long id);
}
