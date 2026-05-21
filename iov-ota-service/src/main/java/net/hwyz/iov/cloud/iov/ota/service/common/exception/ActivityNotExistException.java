package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 升级活动不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class ActivityNotExistException extends OtaBaseException {

    private static final int ERROR_CODE = 411004;

    public ActivityNotExistException(Long activityId) {
        super(ERROR_CODE);
        log.warn("升级活动[{}]不存在", activityId);
    }

}
