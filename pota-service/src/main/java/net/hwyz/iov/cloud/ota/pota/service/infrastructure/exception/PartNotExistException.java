package net.hwyz.iov.cloud.ota.pota.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotExistException extends PotaBaseException {

    public PartNotExistException(String pn) {
        super(ERROR_CODE_PART_NOT_EXIST);
        logger.warn("零件[{}]不存在", pn);
    }

}
