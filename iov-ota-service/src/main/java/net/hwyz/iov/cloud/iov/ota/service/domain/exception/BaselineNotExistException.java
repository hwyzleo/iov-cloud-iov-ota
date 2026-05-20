package net.hwyz.iov.cloud.iov.ota.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 基线不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class BaselineNotExistException extends FotaBaseException {

    private static final int ERROR_CODE = 411003;

    public BaselineNotExistException(String baselineCode) {
        super(ERROR_CODE);
        logger.warn("基线[{}]不存在", baselineCode);
    }

}
