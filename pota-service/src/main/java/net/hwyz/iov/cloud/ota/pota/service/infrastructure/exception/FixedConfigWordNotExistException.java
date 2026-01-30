package net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 固定配置字不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class FixedConfigWordNotExistException extends PotaBaseException {

    public FixedConfigWordNotExistException(Long fixedConfigWordId) {
        super(ERROR_CODE_FIXED_CONFIG_WORD_NOT_EXIST);
        logger.warn("固定配置字[{}]不存在", fixedConfigWordId);
    }

}
