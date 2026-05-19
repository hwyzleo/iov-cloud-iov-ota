package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 兼容零件号不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class CompatiblePnNotExistException extends PotaBaseException {

    public CompatiblePnNotExistException(Long compatiblePnId) {
        super(ERROR_CODE_COMPATIBLE_PN_NOT_EXIST);
        log.warn("兼容零件号[{}]不存在", compatiblePnId);
    }

}
