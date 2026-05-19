package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.PotaBaseException;

/**
 * 软件内部版本不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class SoftwareBuildVersionNotExistException extends PotaBaseException {

    public SoftwareBuildVersionNotExistException(Long softwareBuildVersionId) {
        super(ERROR_CODE_SOFTWARE_BUILD_VERSION_NOT_EXIST);
        log.warn("软件内部版本[{}]不存在", softwareBuildVersionId);
    }

}
