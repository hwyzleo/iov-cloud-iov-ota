package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.PotaBaseException;

/**
 * 零件不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotExistException extends PotaBaseException {

    public PartNotExistException(String pn) {
        super(ERROR_CODE_PART_NOT_EXIST);
        log.warn("零件[{}]不存在", pn);
    }

}
