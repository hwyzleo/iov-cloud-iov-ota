package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.UserConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.UserConsentPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户授权应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConsentAppService {

    private final UserConsentMapper userConsentDao;

    public List<UserConsentPo> search(Long taskId, String vin) {
        if (taskId != null) {
            return userConsentDao.selectByTaskId(taskId);
        }
        if (vin != null && !vin.isBlank()) {
            return userConsentDao.selectByVin(vin);
        }
        return List.of();
    }

    public UserConsentPo getUserConsentById(Long id) {
        return userConsentDao.selectById(id);
    }

}
