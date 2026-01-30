package net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 软件内部版本不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class SoftwareBuildVersionNotExistException extends PotaBaseException {

    public SoftwareBuildVersionNotExistException(Long softwareBuildVersionId) {
        super(ERROR_CODE_SOFTWARE_BUILD_VERSION_NOT_EXIST);
        logger.warn("软件内部版本[{}]不存在", softwareBuildVersionId);
    }

}
