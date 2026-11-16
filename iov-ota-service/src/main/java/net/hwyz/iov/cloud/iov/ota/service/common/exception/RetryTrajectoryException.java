package net.hwyz.iov.cloud.iov.ota.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 重试/续传轨迹冲突（IOV-OTA-DSN-CR-026 §6.6、错误码文档）。
 *
 * <p>错误码段：403000～403999（IOV-OTA 项目段）。同一 {@code retryEventId} 对应不同
 * 重试/续传事实时为确定性冲突，不得覆盖既有不可变事实。
 *
 * @author hwyz_leo
 */
public class RetryTrajectoryException extends BaseException {

    /** 403015：同一 retryEventId 对应不同重试/续传事实。 */
    public static final int ERROR_CODE_CONFLICT = 403015;

    public RetryTrajectoryException(String message) {
        super(ERROR_CODE_CONFLICT, message);
    }
}
