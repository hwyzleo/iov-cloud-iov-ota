package net.hwyz.iov.cloud.iov.ota.service.domain.exception;

/**
 * 多任务放行门禁异常（CR-015）
 * <p>前序正式报告缺失 / 门禁 FAIL / PENDING 时 fail-safe 拦截发布。</p>
 *
 * @author hwyz_leo
 */
public class TaskReleaseGateException extends RuntimeException {

    public TaskReleaseGateException(String message) {
        super(message);
    }

    public TaskReleaseGateException(String message, Throwable cause) {
        super(message, cause);
    }
}
