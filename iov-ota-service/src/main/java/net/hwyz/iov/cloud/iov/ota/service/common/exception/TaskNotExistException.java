package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 升级任务不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class TaskNotExistException extends OtaBaseException {

    private static final int ERROR_CODE = 411001;

    public TaskNotExistException(Long taskId) {
        super(ERROR_CODE);
        log.warn("升级任务[{}]不存在", taskId);
    }

}
